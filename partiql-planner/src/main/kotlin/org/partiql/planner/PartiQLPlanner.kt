package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.plan.Plan
import org.partiql.planner.builder.PartiQLPlannerBuilder
import org.partiql.spi.catalog.Session

/**
 * PartiQLPlanner is responsible for transforming an AST into PartiQL's logical query plan.
 */
public interface PartiQLPlanner {

    /**
     * Transform an AST to a [Plan].
     *
     * @param statement
     * @param session
     * @param onProblem
     * @return
     */
    public fun plan(statement: Statement, session: Session, onProblem: ProblemCallback = {}): Result

    /**
     * Planner result along with any warnings.
     *
     * @property plan
     */
    public class Result(
        public val plan: Plan,
        public val problems: List<Problem>,
    )

    public companion object {

        @JvmStatic
        public fun builder(): PartiQLPlannerBuilder = PartiQLPlannerBuilder()

        @JvmStatic
        public fun standard(): PartiQLPlanner = PartiQLPlannerBuilder().build()
    }
}
