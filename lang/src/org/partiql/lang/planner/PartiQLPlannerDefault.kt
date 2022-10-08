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
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.eval.visitors.FromSourceAliasVisitorTransform
import org.partiql.lang.eval.visitors.GroupByItemAliasVisitorTransform
import org.partiql.lang.eval.visitors.GroupByPathExpressionVisitorTransform
import org.partiql.lang.eval.visitors.OrderBySortSpecVisitorTransform
import org.partiql.lang.eval.visitors.PipelinedVisitorTransform
import org.partiql.lang.eval.visitors.SelectListGroupKeysVisitorTransform
import org.partiql.lang.eval.visitors.SelectListItemAliasVisitorTransform
import org.partiql.lang.eval.visitors.SelectStarVisitorTransform
import org.partiql.lang.planner.transforms.AstToLogicalVisitorTransform
import org.partiql.lang.planner.transforms.LogicalResolvedToDefaultPhysicalVisitorTransform
import org.partiql.lang.planner.transforms.LogicalToLogicalResolvedVisitorTransform
import org.partiql.lang.planner.transforms.allocateVariableIds
import org.partiql.pig.runtime.asPrimitive

internal class PartiQLPlannerDefault(
    private val globalVariableResolver: GlobalVariableResolver,
    private val physicalPlanPasses: List<PartiQLPlannerPass.Physical>,
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

        // Step 2. AST -> LogicalPlan
        val logicalPlan = callback.doEvent("ast_to_logical", normalized) {
            normalized.toLogicalPlan(problemHandler)
        }
        if (problemHandler.hasErrors) {
            return PartiQLPlanner.Result.Error(problemHandler.problems)
        }

        // Step 3. Replace variable references
        val resolvedLogicalPlan = callback.doEvent("logical_to_logical_resolved", logicalPlan) {
            logicalPlan.toResolvedPlan(problemHandler)
        }
        if (problemHandler.hasErrors) {
            return PartiQLPlanner.Result.Error(problemHandler.problems)
        }

        // Step 4. LogicalPlan -> PhysicalPlan
        val physicalPlan = callback.doEvent("logical_resolved_to_physical", resolvedLogicalPlan) {
            resolvedLogicalPlan.toPhysicalPlan(problemHandler)
        }
        if (problemHandler.hasErrors) {
            return PartiQLPlanner.Result.Error(problemHandler.problems)
        }

        // Step 5. Apply additional physical transformations
        val plan = physicalPlanPasses.fold(physicalPlan) { plan, pass ->
            val result = callback.doEvent("pass_${pass::class.java.simpleName}", plan) {
                pass.apply(plan, problemHandler)
            }
            if (problemHandler.hasErrors) {
                return PartiQLPlanner.Result.Error(problemHandler.problems)
            }
            result
        }

        return PartiQLPlanner.Result.Success(
            plan = plan,
            warnings = problemHandler.problems
        )
    }

    // --- Internal --------------------------

    /**
     * AST Normalization Passes
     *  1. Synthesizes unspecified `SELECT <expr> AS ...` aliases
     *  2. Synthesizes unspecified `FROM <expr> AS ...` aliases
     *  3. Changes `SELECT * FROM a, b` to SELECT a.*, b.* FROM a, b`
     */
    private fun PartiqlAst.Statement.normalize(problems: ProblemCollector): PartiqlAst.Statement {
        val transform = PipelinedVisitorTransform(
            SelectListItemAliasVisitorTransform(),
            FromSourceAliasVisitorTransform(),
            OrderBySortSpecVisitorTransform(),
            GroupByItemAliasVisitorTransform(),
            GroupByPathExpressionVisitorTransform(),
            SelectListGroupKeysVisitorTransform(),
            SelectStarVisitorTransform()
        )
        return transform.transformStatement(this)
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
     * See [LogicalResolvedToDefaultPhysicalVisitorTransform]
     */
    private fun PartiqlLogicalResolved.Plan.toPhysicalPlan(problems: ProblemCollector): PartiqlPhysical.Plan {
        val transform = LogicalResolvedToDefaultPhysicalVisitorTransform(problems)
        return transform.transformPlan(this)
    }
}
