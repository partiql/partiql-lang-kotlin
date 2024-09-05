package org.partiql.eval

import org.partiql.eval.builder.PartiQLEngineBuilder
import org.partiql.plan.v1.PartiQLPlan
import org.partiql.planner.catalog.Session

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
 *
 *
 * TODO rename PartiQLEngine to PartiQLCompiler as it produces the statement (statement holds its own execution logic).
 */
public interface PartiQLEngine {

    public fun prepare(plan: PartiQLPlan, mode: Mode, session: Session): PartiQLStatement

    companion object {

        @JvmStatic
        public fun builder(): PartiQLEngineBuilder = PartiQLEngineBuilder()

        @JvmStatic
        fun standard() = PartiQLEngineBuilder().build()
    }

    /**
     * TODO move mode to the session ??
     */
    public enum class Mode {
        PERMISSIVE,
        STRICT // AKA, Type Checking Mode in the PartiQL Specification
    }
}
