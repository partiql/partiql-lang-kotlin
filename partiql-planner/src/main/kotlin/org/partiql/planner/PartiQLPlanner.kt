package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.plan.PartiQLPlan
import org.partiql.planner.metadata.Namespace
import java.time.Instant

/**
 * PartiQLPlanner is responsible for transforming an AST into PartiQL's logical query plan.
 */
public interface PartiQLPlanner {

    /**
     * Transform an AST to a [PartiQLPlan].
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
        public val plan: PartiQLPlan,
        public val problems: List<Problem>,
    )

    /**
     * From [org.partiql.lang.planner.transforms]
     *
     * @property queryId
     * @property userId
     * @property currentCatalog
     * @property currentDirectory
     * @property catalogs
     * @property instant
     */
    public class Session(
        public val queryId: String,
        public val userId: String,
        public val currentCatalog: String,
        public val currentDirectory: List<String> = emptyList(),
        public val catalogs: Map<String, Namespace> = emptyMap(),
        public val instant: Instant = Instant.now(),
    )
    public companion object {

        @JvmStatic
        public fun builder(): PartiQLPlannerBuilder = PartiQLPlannerBuilder()

        @JvmStatic
        public fun default(): PartiQLPlanner = PartiQLPlannerBuilder().build()
    }
}
