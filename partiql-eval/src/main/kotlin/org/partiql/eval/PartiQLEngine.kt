package org.partiql.eval

import org.partiql.plan.PartiQLPlan

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
        public fun builder(): PartiQLEngineBuilder = PartiQLEngineBuilder()

        @JvmStatic
        fun default() = PartiQLEngineBuilder().build()
    }
}
