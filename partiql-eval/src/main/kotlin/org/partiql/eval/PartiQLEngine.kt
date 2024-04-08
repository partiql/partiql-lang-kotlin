package org.partiql.eval

import org.partiql.plan.PartiQLPlan
import org.partiql.spi.connector.Connector

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

    public fun prepare(plan: PartiQLPlan, session: Session): PartiQLStatement<*>

    // TODO: Pass session variable during statement execution once we finalize data structure for session.
    public fun execute(statement: PartiQLStatement<*>): PartiQLResult

    companion object {

        @JvmStatic
        public fun builder(): PartiQLEngineBuilder = PartiQLEngineBuilder()

        @JvmStatic
        fun default() = PartiQLEngineBuilder().build()
    }

    public class Session(
        val catalogs: Map<String, Connector> = mapOf(),
        val mode: Mode = Mode.PERMISSIVE,
        val errorHandling: CompilationErrorHandling = CompilationErrorHandling.SIGNALING
    )

    /**
     * This determines the behavior when the evaluator encounters scenarios in which a type check exception occurs.
     */
    public enum class Mode {
        /**
         * Returns MISSING when a type check exception occurs.
         */
        PERMISSIVE,

        /**
         * Propagates the type check exception.
         */
        STRICT // AKA, Type Checking Mode in the PartiQL Specification
    }

    /**
     * When the PartiQL Plan has determined that a function call or variable reference will always error, the
     * [CompilationErrorHandling] will determine how the internal compiler will treat the error. Note that this is subtly
     * different than [Mode]. [CompilationErrorHandling] is specifically used when nodes are known to ALWAYS return
     * MISSING. The difference can be understood as compile-time ([CompilationErrorHandling]) vs run-time ([Mode]).
     */
    public enum class CompilationErrorHandling {
        /**
         * Returns a literal MISSING.
         */
        QUIET,

        /**
         * Errors out.
         */
        SIGNALING
    }
}
