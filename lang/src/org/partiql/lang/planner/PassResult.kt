package org.partiql.lang.planner
import org.partiql.lang.errors.Problem

sealed class PassResult<TResult> {
    /**
     * Indicates query planning was successful and includes a list of any warnings that were encountered along the way.
     */
    data class Success<TResult>(val result: TResult, val warnings: List<Problem>) : PassResult<TResult>()

    /**
     * Indicates query planning was not successful and includes a list of errors and warnings that were encountered
     * along the way.  Encountering both errors and warnings, as well as multiple errors is possible since we are not
     * required to stop when encountering the first error.
     */
    data class Error<TResult>(val errors: List<Problem>) : PassResult<TResult>()
}
