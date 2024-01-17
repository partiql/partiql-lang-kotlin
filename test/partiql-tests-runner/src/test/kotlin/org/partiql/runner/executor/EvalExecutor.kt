package org.partiql.runner.executor

import com.amazon.ion.IonInt
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.errors.TypeCheckException
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.lang.eval.CompileOptions
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.plugin.PartiQLPlugin
import org.partiql.plugins.memory.MemoryBindings
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.StaticType
import org.partiql.value.BoolValue
import org.partiql.value.CharValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.IntValue
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.ScalarValue
import org.partiql.value.check
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.toIon
import java.math.BigInteger

@OptIn(PartiQLValueExperimental::class)
class EvalExecutor(
    private val plannerSession: PartiQLPlanner.Session,
    private val evalSession: PartiQLEngine.Session
) : TestExecutor<PartiQLStatement<*>, PartiQLResult> {

    override fun prepare(statement: String): PartiQLStatement<*> {
        val stmt = parser.parse(statement).root
        val plan = planner.plan(stmt, plannerSession)
        return engine.prepare(plan.plan, evalSession)
    }

    override fun execute(statement: PartiQLStatement<*>): PartiQLResult {
        return engine.execute(statement)
    }

    override fun fromIon(value: IonValue): PartiQLResult {
        val partiql = PartiQLValueIonReaderBuilder.standard().build(value.toIonElement()).read()

        return PartiQLResult.Value(partiql)
    }

    override fun toIon(value: PartiQLResult): IonValue {
        if (value is PartiQLResult.Value) {
            return value.value.toIon().toIonValue(ION)
        }
        error("PartiQLResult cannot be converted to Ion")
    }

    override fun compare(actual: PartiQLResult, expect: PartiQLResult): Boolean {
        if (actual is PartiQLResult.Value && expect is PartiQLResult.Value) {
            return valueComparison(actual.value, expect.value)
        }
        error("Cannot compare different types of PartiQLResult")
    }

    // Value comparison of PartiQL Value that utilized Ion Hashcode.
    // in here, null.bool is considered equivalent to null
    // missing is considered different from null
    // annotation::1 is considered different from 1
    // 1 of type INT is considered the same as 1 of type INT32
    // we should probably consider adding our own hashcode implementation
    private fun valueComparison(v1: PartiQLValue, v2: PartiQLValue): Boolean {
        if (v1.isNull && v2.isNull) {
            return true
        }
        if (v1 == v2) {
            return true
        }
        if (v1.toIon().hashCode() == v2.toIon().hashCode()) {
            return true
        }
        // Ion element hash code contains a bug
        // Hashcode of BigIntIntElementImpl(BigInteger.ONE) is not the same as that of LongIntElementImpl(1)
        if (v1.toIon().type == ElementType.INT && v2.toIon().type == ElementType.INT) {
            return v1.toIon().asAnyElement().bigIntegerValue == v2.toIon().asAnyElement().bigIntegerValue
        }
        return false
    }
    companion object {
        val parser = PartiQLParser.default()
        val planner = PartiQLPlanner.default()
        val engine = PartiQLEngine.default()
    }

    object Factory : TestExecutor.Factory<PartiQLStatement<*>, PartiQLResult> {

        @OptIn(PartiQLFunctionExperimental::class)
        override fun create(env: IonStruct, options: CompileOptions): TestExecutor<PartiQLStatement<*>, PartiQLResult> {
            val catalog = "default"
            val data = env.toIonElement() as StructElement

            // infer catalog from env
            val connector = infer(data)

            val session = PartiQLPlanner.Session(
                queryId = "query",
                userId = "user",
                currentCatalog = catalog,
                catalogs = mapOf(
                    "default" to connector.getMetadata(object : ConnectorSession {
                        override fun getQueryId(): String = "query"
                        override fun getUserId(): String = "user"
                    })
                )
            )

            val evalSession = PartiQLEngine.Session(
                bindings = mutableMapOf(
                    "default" to connector.getBindings()
                ),
                functions = mutableMapOf(
                    "partiql" to PartiQLPlugin.functions
                )
            )
            return EvalExecutor(session, evalSession)
        }

        /**
         * Produces an inferred catalog from the environment.
         * Until this point, PartiQL Kotlin has only done top-level bindings.
         *
         * @param env
         * @return
         */
        private fun infer(env: StructElement): Connector {
            val map = mutableMapOf<String, StaticType>()
            env.fields.forEach {
                map[it.name] = StaticType.ANY
            }
            val metadata = MemoryConnector.Metadata(map)
            val globals = load(metadata, env)
            val bindings = MemoryBindings(globals)
            return MemoryConnector(metadata, bindings)
        }

        /**
         * Loads each declared global of the catalog from the data element.
         */
        private fun load(metadata: MemoryConnector.Metadata, data: StructElement): Map<String, PartiQLValue> {
            val bindings = mutableMapOf<String, PartiQLValue>()
            for ((key, _) in metadata.entries) {
                var ion: IonElement = data
                val steps = key.split(".")
                steps.forEach { s ->
                    if (ion is StructElement) {
                        ion = (ion as StructElement).getOptional(s) ?: error("No value for binding $key")
                    } else {
                        error("No value for binding $key")
                    }
                }
                bindings[key] = PartiQLValueIonReaderBuilder.standard().build(ion).read()
            }
            return bindings
        }
    }
}
