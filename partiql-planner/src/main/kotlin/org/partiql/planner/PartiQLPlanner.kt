package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.plan.Plan
import org.partiql.planner.builder.PartiQLPlannerBuilder
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PErrorListenerException
import kotlin.jvm.Throws

/**
 * PartiQLPlanner is responsible for transforming an AST into PartiQL's logical query plan.
 */
public interface PartiQLPlanner {

    /**
     * Transform an AST to a [Plan].
     *
     * @param statement
     * @param session
     * @param ctx a configuration object
     * @return
     */
    @Throws(PErrorListenerException::class)
    public fun plan(statement: Statement, session: Session, ctx: Context): Result

    /**
     * Transform an AST to a [Plan].
     *
     * @param statement
     * @param session
     * @return
     */
    @Throws(PErrorListenerException::class)
    public fun plan(statement: Statement, session: Session): Result {
        return plan(statement, session, Context.standard())
    }

    /**
     * Planner result.
     *
     * @property plan
     */
    public class Result(
        public val plan: Plan,
    )

    public companion object {

        /**
         * Create a new [PartiQLPlannerBuilder] instance.
         *
         * @return
         */
        @JvmStatic
        public fun builder(): PartiQLPlannerBuilder = PartiQLPlannerBuilder()

        /**
         * Create a new [PartiQLPlanner] instance with the default configuration.
         *
         * @return
         */
        @JvmStatic
        public fun standard(): PartiQLPlanner = PartiQLPlannerBuilder().build()
    }
}
