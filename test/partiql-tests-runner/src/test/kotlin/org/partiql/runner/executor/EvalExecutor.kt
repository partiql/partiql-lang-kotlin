package org.partiql.runner.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.lang.eval.CompileOptions
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.plugins.memory.MemoryBindings
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.toIon

@OptIn(PartiQLValueExperimental::class)
class EvalExecutor(
    private val connector: Connector,
    private val session: PartiQLPlanner.Session,
) : TestExecutor<PartiQLStatement<*>, PartiQLResult> {

    private val planner = PartiQLPlannerBuilder()
        .addCatalog(
            "test",
            connector.getMetadata(object : ConnectorSession {
                override fun getQueryId(): String = session.queryId
                override fun getUserId(): String = session.userId
            })
        )
        .build()

    override fun prepare(statement: String): PartiQLStatement<*> {
        val stmt = parser.parse(statement).root
        val plan = planner.plan(stmt, session)
        return engine.prepare(plan.plan)
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
            return actual.value == expect.value
        }
        error("Cannot compare different types of PartiQLResult")
    }

    companion object {
        val parser = PartiQLParser.default()
        val engine = PartiQLEngine.default()
    }

    object Factory : TestExecutor.Factory<PartiQLStatement<*>, PartiQLResult> {

        override fun create(env: IonStruct, options: CompileOptions): TestExecutor<PartiQLStatement<*>, PartiQLResult> {
            val catalog = "default"
            val data = env.toIonElement() as StructElement

            // infer catalog from env
            val connector = infer(data)

            val session = PartiQLPlanner.Session(
                queryId = "query",
                userId = "user",
                currentCatalog = catalog,
            )
            return EvalExecutor(connector, session)
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
