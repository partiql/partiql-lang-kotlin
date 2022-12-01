/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.util.PartiQLExperimental

/**
 * [PartiQLPlanner] is responsible for transforming a [PartiqlAst.Statement] representation of a query into an
 * equivalent [PartiqlPhysical.Plan] representation of the query.
 */
@PartiQLExperimental
interface PartiQLPlanner {

    /**
     * Transforms the given statement to an equivalent expression tree with each SELECT-FROM-WHERE block
     * expanded into its relational algebra form.
     *
     * If planning succeeds, this returns a [PartiQLPlanner.Result.Success],
     * Else this returns a [PartiQLPlanner.Result.Error].
     *
     * TODO this error handling pattern is subject to review and change
     */
    fun plan(statement: PartiqlAst.Statement): Result

    companion object {
        const val PLAN_VERSION = "0.0"
    }

    /**
     * TODO move Result.Success/Result.Error variant to the PartiQLCompiler
     */
    sealed class Result {

        data class Success @JvmOverloads constructor(
            val plan: PartiqlPhysical.Plan,
            val warnings: List<Problem>,
            val details: PlanningDetails = PlanningDetails()
        ) : Result()

        data class Error(val problems: List<Problem>) : Result() {
            override fun toString(): String = problems.joinToString()
        }
    }

    data class PlanningDetails(
        val ast: PartiqlAst.Statement? = null,
        val astNormalized: PartiqlAst.Statement? = null,
        val logical: PartiqlLogical.Plan? = null,
        val logicalResolved: PartiqlLogicalResolved.Plan? = null,
        val physical: PartiqlPhysical.Plan? = null,
        val physicalTransformed: PartiqlPhysical.Plan? = null
    )

    /**
     * Options which control [PartiQLPlanner] behavior.
     */
    class Options(
        val allowedUndefinedVariables: Boolean = false,
        val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.HONOR_PARAMETERS
    )
}
