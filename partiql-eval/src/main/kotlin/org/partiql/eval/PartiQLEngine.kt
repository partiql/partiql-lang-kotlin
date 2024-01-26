package org.partiql.eval

import org.partiql.plan.PartiQLPlan
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental

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

    public class Session @OptIn(PartiQLFunctionExperimental::class) constructor(
        val bindings: Map<String, ConnectorBindings> = mapOf(),
        val functions: Map<String, List<PartiQLFunction>> = mapOf(),
        val mode: Mode = Mode.PERMISSIVE
    )

    public enum class Mode {
        PERMISSIVE,
        STRICT // AKA, Type Checking Mode in the PartiQL Specification
    }
}
