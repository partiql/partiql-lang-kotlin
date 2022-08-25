package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem

interface PartiQLPlanner {

    fun plan(statement: PartiqlAst.Statement): Result

    companion object {
        const val PLAN_VERSION = "0.0"
    }

    /**
     * This method of error handling will be reviewed by the team and is subject to change.
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
