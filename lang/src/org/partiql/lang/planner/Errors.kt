package org.partiql.lang.planner

import org.partiql.lang.ast.UNKNOWN_SOURCE_LOCATION
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.eval.physical.sourceLocationMeta
import org.partiql.pig.runtime.DomainNode

/**
 * Convenience function that logs [PlanningProblemDetails.UnimplementedFeature] to the receiver [ProblemHandler]
 * handler, putting [blame] on the specified [DomainNode] (this is the superclass of all PIG-generated types).
 * "Blame" in this case, means that the line & column number in the metas of [blame] become the problem's.
 */
fun ProblemHandler.handleUnimplementedFeature(blame: DomainNode, featureName: String) =
    this.handleProblem(createUnimplementedFeatureProblem(blame, featureName))

private fun createUnimplementedFeatureProblem(blame: DomainNode, featureName: String) =
    Problem(
        blame.metas.sourceLocationMeta ?: UNKNOWN_SOURCE_LOCATION,
        PlanningProblemDetails.UnimplementedFeature(featureName)
    )
