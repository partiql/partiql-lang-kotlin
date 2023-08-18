/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.planner

import org.partiql.errors.Problem
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.planner.impl.PartiQLPlannerDefault

/**
 * [PartiQLPlanner] is responsible for transforming a [PartiqlAst.Statement] representation of a query into an
 * equivalent [PartiqlLogicalResolved.Plan] representation of the query.
 */
@ExperimentalPartiQLPlanner
public interface PartiQLPlanner {

    /**
     * Transforms the given statement to an equivalent expression tree with each SELECT-FROM-WHERE block
     * expanded into its relational algebra form.
     *
     * If planning succeeds, this returns a [PartiQLPlanner.Result.Success],
     * Else this returns a [PartiQLPlanner.Result.Error].
     *
     * TODO this error handling pattern is subject to review and change
     */
    public fun plan(statement: PartiqlAst.Statement): Result

    public companion object {
        public const val PLAN_VERSION: String = "0.0"
    }

    /**
     * TODO move Result.Success/Result.Error variant to the PartiQLCompiler
     */
    public sealed class Result {

        public data class Success @JvmOverloads constructor(
            val plan: PartiqlLogicalResolved.Plan,
            val warnings: List<Problem>,
            val details: PlanningDetails = PlanningDetails()
        ) : Result()

        public data class Error(val problems: List<Problem>) : Result() {
            override fun toString(): String = problems.joinToString()
        }
    }

    public data class PlanningDetails(
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
    public class Options(
        public val allowedUndefinedVariables: Boolean = false
    )

    /**
     * Builder class to instantiate a [PartiQLPhysicalPlanner].
     */
    @ExperimentalPartiQLPlanner
    public class Builder private constructor() {

        private var globalVariableResolver = GlobalVariableResolver.EMPTY
        private var callback: PlannerEventCallback? = null
        private var options = Options()

        public companion object {

            @JvmStatic
            public fun standard(): Builder = Builder()
        }

        public fun globalVariableResolver(globalVariableResolver: GlobalVariableResolver): Builder = this.apply {
            this.globalVariableResolver = globalVariableResolver
        }

        public fun options(options: Options): Builder = this.apply {
            this.options = options
        }

        public fun callback(callback: PlannerEventCallback): Builder = this.apply {
            this.callback = callback
        }

        public fun build(): PartiQLPlanner = PartiQLPlannerDefault(
            globalVariableResolver = globalVariableResolver,
            callback = callback,
            options = options
        )
    }
}
