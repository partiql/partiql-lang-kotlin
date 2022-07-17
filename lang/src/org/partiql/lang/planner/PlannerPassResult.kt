package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem

sealed class PlannerPassResult<TResult> {
    /**
     * Indicates query planning was successful and includes a list of any warnings that were encountered along the way.
     *
     * - [output] The main output of the planner pass, i.e. a transformed instance of
     * [PartiqlLogical.Plan], [PartiqlPhysical.Plan] or [QueryPlan]. Both are possible outputs of a different types of
     * different kinds of planner planner passes.
     * - [warnings] A list of warnings that were encountered while processing the pass's input and producing its
     * output.  A pass may still be considered successful if it produces warnings.
     */
    data class Success<TResult>(val output: TResult, val warnings: List<Problem>) : PlannerPassResult<TResult>()

    /**
     * Indicates query planning was not successful and includes a list of errors and warnings that were encountered
     * along the way.  Encountering both errors and warnings, as well as multiple errors is possible since we are not
     * required to stop when encountering the first error.
     */
    data class Error<TResult>(val errors: List<Problem>) : PlannerPassResult<TResult>()
}
