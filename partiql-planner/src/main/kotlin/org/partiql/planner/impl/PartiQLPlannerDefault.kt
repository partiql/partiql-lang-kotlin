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

package org.partiql.planner.impl

import org.partiql.errors.Problem
import org.partiql.errors.ProblemHandler
import org.partiql.errors.ProblemSeverity
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.pig.runtime.asPrimitive
import org.partiql.planner.ExperimentalPartiQLPlanner
import org.partiql.planner.GlobalVariableResolver
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PlannerEventCallback
import org.partiql.planner.transforms.AggregationVisitorTransform
import org.partiql.planner.transforms.AstToLogicalVisitorTransform
import org.partiql.planner.transforms.FromSourceAliasVisitorTransform
import org.partiql.planner.transforms.LogicalToLogicalResolvedVisitorTransform
import org.partiql.planner.transforms.OrderBySortSpecVisitorTransform
import org.partiql.planner.transforms.PipelinedVisitorTransform
import org.partiql.planner.transforms.SelectListItemAliasVisitorTransform
import org.partiql.planner.transforms.SelectStarVisitorTransform
import org.partiql.planner.transforms.SubqueryCoercionVisitorTransform
import org.partiql.planner.validators.PartiqlAstSanityValidator
import org.partiql.planner.validators.PartiqlLogicalResolvedValidator
import org.partiql.planner.validators.PartiqlLogicalValidator

@OptIn(ExperimentalPartiQLPlanner::class)
internal class PartiQLPlannerDefault(
    private val globalVariableResolver: GlobalVariableResolver,
    private val callback: PlannerEventCallback?,
    private val options: PartiQLPlanner.Options
) : PartiQLPlanner {

    override fun plan(statement: PartiqlAst.Statement): PartiQLPlanner.Result {

        val problemHandler = ProblemCollector()

        // Step 1. Normalize the AST
        val normalized = callback.doEvent("normalize_ast", statement) {
            statement.normalize(problemHandler)
        }
        if (problemHandler.hasErrors) {
            return PartiQLPlanner.Result.Error(problemHandler.problems)
        }
        normalized.validate()

        // Step 2. AST -> LogicalPlan
        val logicalPlan = callback.doEvent("ast_to_logical", normalized) {
            normalized.toLogicalPlan(problemHandler)
        }
        if (problemHandler.hasErrors) {
            return PartiQLPlanner.Result.Error(problemHandler.problems)
        }
        // Validate logical plan
        // TODO: if it is an invalid logical plan, do we want to add it to [problemHandler]?
        PartiqlLogicalValidator().walkPlan(logicalPlan)

        // Step 3. Replace variable references
        val plan = callback.doEvent("logical_to_logical_resolved", logicalPlan) {
            logicalPlan.toResolvedPlan(problemHandler)
        }
        if (problemHandler.hasErrors) {
            return PartiQLPlanner.Result.Error(problemHandler.problems)
        }
        PartiqlLogicalResolvedValidator().walkPlan(plan)

        return PartiQLPlanner.Result.Success(
            plan = plan,
            warnings = problemHandler.problems,
            details = PartiQLPlanner.PlanningDetails(
                ast = statement,
                astNormalized = normalized,
                logical = logicalPlan,
                logicalResolved = plan
            )
        )
    }

    // --- Internal --------------------------

    /**
     * AST Normalization Passes
     */
    @Suppress("UNUSED_PARAMETER") // future work?
    private fun PartiqlAst.Statement.normalize(problems: ProblemCollector): PartiqlAst.Statement {
        val transform = PipelinedVisitorTransform(
            SelectListItemAliasVisitorTransform(),
            FromSourceAliasVisitorTransform(),
            OrderBySortSpecVisitorTransform(),
            AggregationVisitorTransform(),
            SelectStarVisitorTransform(),
            SubqueryCoercionVisitorTransform(),
        )
        return transform.transformStatement(this)
    }

    /**
     * Performs a validation of the AST.
     */
    private fun PartiqlAst.Statement.validate() {
        PartiqlAstSanityValidator().validate(this)
    }

    /**
     * See [AstToLogicalVisitorTransform]
     */
    private fun PartiqlAst.Statement.toLogicalPlan(problems: ProblemCollector): PartiqlLogical.Plan {
        val transform = AstToLogicalVisitorTransform(problems)
        return PartiqlLogical.Plan(
            stmt = transform.transformStatement(this),
            version = PartiQLPlanner.PLAN_VERSION.asPrimitive()
        )
    }

    /**
     * See [LogicalToLogicalResolvedVisitorTransform]
     */
    private fun PartiqlLogical.Plan.toResolvedPlan(problems: ProblemCollector): PartiqlLogicalResolved.Plan {
        val (planWithAllocatedVariables, allLocals) = this.allocateVariableIds()
        val transform = LogicalToLogicalResolvedVisitorTransform(
            allowUndefinedVariables = options.allowedUndefinedVariables,
            problemHandler = problems,
            globals = globalVariableResolver,
        )
        return transform.transformPlan(planWithAllocatedVariables).copy(locals = allLocals)
    }

    /**
     * A [ProblemHandler] that collects all of the encountered [Problem]s without throwing.
     *
     * This is intended to be used when wanting to collect multiple problems that may be encountered (e.g. a static type
     * inference pass that can result in multiple errors and/or warnings). This handler does not collect other exceptions
     * that may be thrown.
     */
    private class ProblemCollector : ProblemHandler {
        private val problemList = mutableListOf<Problem>()

        val problems: List<Problem>
            get() = problemList

        val hasErrors: Boolean
            get() = problemList.any { it.details.severity == ProblemSeverity.ERROR }

        val hasWarnings: Boolean
            get() = problemList.any { it.details.severity == ProblemSeverity.WARNING }

        override fun handleProblem(problem: Problem) {
            problemList.add(problem)
        }
    }
}
