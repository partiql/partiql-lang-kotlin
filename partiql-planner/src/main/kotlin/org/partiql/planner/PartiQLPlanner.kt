package org.partiql.planner

import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.plan.PartiQLPlan
import org.partiql.spi.connector.ConnectorMetadata
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
        public val catalogs: Map<String, ConnectorMetadata> = emptyMap(),
        public val instant: Instant = Instant.now(),
        public val missingOpBehavior: MissingOpBehavior = MissingOpBehavior.QUIET
    ) {
        /**
         * Determine the planner behavior upon encounter an operation that always returns MISSING. 
         * In both mode, The problometic operation will be tracked in problem callback. 
         * Subsequence opearation will take in MISSING as input. 
         */
        public enum class MissingOpBehavior {
            /**
             *  The problometic operation will be tracked in problem callback as a error. 
             *  The result plan will turn the problemetic operation into an error node. 
             */
            QUIET,
            /**
             * The problometic operation will be tracked in problem callback as a error.
             * The result plan will turn the problemetic operation into an missing node. 
             */
            SIGNAL
        }
    }

    public companion object {

        @JvmStatic
        public fun builder(): PartiQLPlannerBuilder = PartiQLPlannerBuilder()

        @JvmStatic
        public fun default(): PartiQLPlanner = PartiQLPlannerBuilder().build()

        /**
         * A planner that preserves the trace of problemetic operation for the purpose of debugging. 
         */ 
        @JvmStatic
        public fun debug(): PartiQLPlanner = PartiQLPlannerDebug()
    }
}
