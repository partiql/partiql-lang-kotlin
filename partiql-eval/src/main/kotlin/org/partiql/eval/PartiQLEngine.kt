package org.partiql.eval

import org.partiql.eval.internal.Compiler
import org.partiql.eval.internal.Record
import org.partiql.plan.PartiQLPlan
import org.partiql.spi.Plugin
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * PartiQL's Experimental Engine.
 *
 * It represents the execution of queries and does NOT represent the
 * maintenance of an individual's session. For example, by the time the engine is invoked, all functions
 * should be resolved via the SQL Path (which takes into consideration the user's current catalog/schema).
 *
 * This is in contrast to an actual application of PartiQL. Applications of PartiQL should instantiate a
 * [org.partiql.planner.PartiQLPlanner] and should pass in a user's session. This engine has no idea what the session is.
 * It assumes that the [org.partiql.plan.PartiQLPlan] has been resolved to accommodate session specifics.
 *
 * This engine also internalizes the mechanics of the engine itself. Internally, it creates a physical plan to operate on,
 * and it executes directly on that plan. The limited number of APIs exposed in this library is intentional to allow for
 * under-the-hood experimentation by the PartiQL Community.
 */
public interface PartiQLEngine {

    public fun prepare(plan: PartiQLPlan): PartiQLStatement<*>

    public fun execute(statement: PartiQLStatement<*>): PartiQLResult

    companion object {
        @JvmStatic
        @JvmOverloads
        fun default(plugins: List<Plugin> = emptyList()) = Builder().plugins(plugins).build()
    }

    public class Builder {

        private var plugins: List<Plugin> = emptyList()

        public fun plugins(plugins: List<Plugin>): Builder = this.apply {
            this.plugins = plugins
        }

        public fun build(): PartiQLEngine = Default(plugins)
    }

    private class Default(private val plugins: List<Plugin>) : PartiQLEngine {

        @OptIn(PartiQLValueExperimental::class)
        override fun prepare(plan: PartiQLPlan): PartiQLStatement<*> {
            // Close over the expression.
            // Right now we are assuming we only have a query statement hence a value statement.
            val expression = Compiler.compile(plan)
            return object : PartiQLStatement.Query {
                override fun execute(): PartiQLValue {
                    return expression.eval(Record(emptyList()))
                }
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun execute(statement: PartiQLStatement<*>): PartiQLResult {
            return when (statement) {
                is PartiQLStatement.Query -> try {
                    val value = statement.execute()
                    PartiQLResult.Value(value)
                } catch (ex: Exception) {
                    PartiQLResult.Error(ex)
                }
            }
        }
    }
}
