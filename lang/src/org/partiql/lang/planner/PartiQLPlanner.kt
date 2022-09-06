package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem

/**
 * [PartiQLPlanner] is responsible for transforming a [PartiqlAst.Statement] representation of a query into an
 * equivalent [PartiqlPhysical.Plan] representation of the query.
 */
interface PartiQLPlanner {

    /**
     * Transforms the given statement to an equivalent expression tree with each SELECT-FROM-WHERE block
     * expanded into its relational algebra form.
     *
     * If planning succeeds, this returns a PartiQLPlanner.Result.Success,
     * Else this returns a PartiQLPlanner.Result.Error.
     *
     * TODO this error handling pattern is subject to review and change
     */
    fun plan(statement: PartiqlAst.Statement): Result

    companion object {
        const val PLAN_VERSION = "0.0"
    }

    /**
     * TODO review error handling pattern with the team
     */
    sealed class Result {

        data class Success(
            val plan: PartiqlPhysical.Plan,
            val warnings: List<Problem>
        ) : Result()

        data class Error(val problems: List<Problem>) : Result() {
            override fun toString(): String = problems.joinToString()
        }
    }

    class Options(val allowedUndefinedVariables: Boolean = false)
}
