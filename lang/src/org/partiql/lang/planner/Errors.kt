package org.partiql.lang.planner

import org.partiql.lang.ast.UNKNOWN_SOURCE_LOCATION
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.eval.physical.sourceLocationMeta
import org.partiql.pig.runtime.DomainNode

/**
 * Thrown by the planner when a [Problem] with severity set to [ProblemSeverity.FATAL] has been issued to the
 * [ProblemHandler] instance and compilation cannot proceed under any circumstance.
 *
 * The [Problem] which has caused the query to be aborted.
 */
internal class PlanningAbortedException(val problem: Problem) : Exception()

/**
 * Aborts a query planner pass by throwing and exception that is caught by [PlannerPipeline], which also
 * takes care of logging the error.
 */
fun abortQueryPlanning(problem: Problem): Nothing {
    throw PlanningAbortedException(problem)
}

fun abortUnimplementedFeature(blame: DomainNode, featureName: String): Nothing =
    abortQueryPlanning(
        Problem(
            blame.metas.sourceLocationMeta ?: UNKNOWN_SOURCE_LOCATION,
            PlanningProblemDetails.UnimplementedFeature(featureName)
        )
    )

fun abortDisallowedFeature(blame: DomainNode, featureName: String): Nothing =
    abortQueryPlanning(
        Problem(
            blame.metas.sourceLocationMeta ?: UNKNOWN_SOURCE_LOCATION,
            PlanningProblemDetails.DisallowedFeature(featureName)
        )
    )
