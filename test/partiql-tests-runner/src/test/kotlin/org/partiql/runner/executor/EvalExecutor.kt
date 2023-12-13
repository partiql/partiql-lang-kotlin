package org.partiql.runner.executor

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.lang.eval.CompileOptions
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryPlugin
import org.partiql.runner.ION
import org.partiql.runner.test.TestExecutor
import org.partiql.spi.Plugin
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.toIon

@OptIn(PartiQLValueExperimental::class)
class EvalExecutor(
    private val plugin: Plugin,
    private val session: PartiQLPlanner.Session,
) : TestExecutor<PartiQLStatement<*>, PartiQLResult> {

    private val planner = PartiQLPlannerBuilder().plugins(listOf(plugin)).build()

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
        val parser = PartiQLParserBuilder.standard().build()
        val engine = PartiQLEngine.default()
    }

    object Factory : TestExecutor.Factory<PartiQLStatement<*>, PartiQLResult> {

        override fun create(env: IonStruct, options: CompileOptions): TestExecutor<PartiQLStatement<*>, PartiQLResult> {
            val catalog = "default"
            val data = env.toIonElement() as StructElement
            val provider = MemoryCatalog.Provider()

            // infer catalog from env
            provider[catalog] = infer(data)

            val plugin = MemoryPlugin(provider, data)
            val session = PartiQLPlanner.Session(
                queryId = "query",
                userId = "user",
                currentCatalog = catalog,
                catalogConfig = mapOf(
                    catalog to ionStructOf(
                        "connector_name" to ionString("memory")
                    )
                )
            )
            return EvalExecutor(plugin, session)
        }

        /**
         * Produces an inferred catalog from the environment.
         * Until this point, PartiQL Kotlin has only done top-level bindings.
         *
         * @param env
         * @return
         */
        private fun infer(env: StructElement): MemoryCatalog {
            val map = mutableMapOf<String, StaticType>()
            env.fields.forEach {
                map[it.name] = StaticType.ANY
            }
            return MemoryCatalog(map)
        }
    }
}
