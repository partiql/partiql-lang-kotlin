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

import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.planner.transforms.LogicalResolvedToDefaultPhysicalVisitorTransform
import org.partiql.planner.ExperimentalPartiQLPlanner
import org.partiql.planner.GlobalVariableResolver
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PlannerEvent
import org.partiql.planner.PlannerEventCallback
import java.time.Duration
import java.time.Instant

@ExperimentalPartiQLCompilerPipeline
@OptIn(ExperimentalPartiQLPlanner::class)
internal class PartiQLPhysicalPlannerDefault(
    globalVariableResolver: GlobalVariableResolver,
    private val physicalPlanPasses: List<PartiQLPhysicalPass>,
    private val callback: PlannerEventCallback?,
    options: PartiQLPhysicalPlanner.Options
) : PartiQLPhysicalPlanner {

    private val logicalPlanner = when (callback) {
        null -> PartiQLPlanner.Builder.standard()
            .globalVariableResolver(globalVariableResolver)
            .options(options = options.toLogicalOptions())
            .build()
        else -> PartiQLPlanner.Builder.standard()
            .globalVariableResolver(globalVariableResolver)
            .options(options = options.toLogicalOptions())
            .callback(callback)
            .build()
    }

    private fun PartiQLPhysicalPlanner.Options.toLogicalOptions() = PartiQLPlanner.Options(
        allowedUndefinedVariables = this.allowedUndefinedVariables
    )

    override fun plan(statement: PartiqlAst.Statement): PartiQLPhysicalPlanner.Result {
        // Step 1. Invoke Logical Planner
        val planResult = logicalPlanner.plan(statement)
        val plan = when (planResult) {
            is PartiQLPlanner.Result.Error -> return PartiQLPhysicalPlanner.Result.Error(planResult.problems)
            is PartiQLPlanner.Result.Success -> planResult.plan
        }

        // Step 2. LogicalPlan -> PhysicalPlan
        val problemHandler = ProblemCollector()
        val physicalPlan = callback.doEvent("logical_resolved_to_physical", plan) {
            plan.toPhysicalPlan(problemHandler)
        }
        if (problemHandler.hasErrors) {
            return PartiQLPhysicalPlanner.Result.Error(problemHandler.problems)
        }

        // Step 3. Apply additional physical transformations
        val physicalPlanTransformed = physicalPlanPasses.fold(physicalPlan) { currentPhysicalPlan, pass ->
            val result = callback.doEvent("pass_${pass::class.java.simpleName}", currentPhysicalPlan) {
                pass.apply(currentPhysicalPlan, problemHandler)
            }
            if (problemHandler.hasErrors) {
                return PartiQLPhysicalPlanner.Result.Error(problemHandler.problems)
            }
            result
        }

        return PartiQLPhysicalPlanner.Result.Success(
            plan = physicalPlanTransformed,
            warnings = problemHandler.problems,
            details = PartiQLPhysicalPlanner.PlanningDetails(
                ast = statement,
                astNormalized = planResult.details.astNormalized,
                logical = planResult.details.logical,
                logicalResolved = plan,
                physical = physicalPlan,
                physicalTransformed = physicalPlanTransformed
            )
        )
    }

    // --- Internal --------------------------

    /**
     * See [LogicalResolvedToDefaultPhysicalVisitorTransform]
     */
    private fun PartiqlLogicalResolved.Plan.toPhysicalPlan(problems: ProblemCollector): PartiqlPhysical.Plan {
        val transform = LogicalResolvedToDefaultPhysicalVisitorTransform(problems)
        return transform.transformPlan(this)
    }

    /** Convenience function for optionally invoking [PlannerEventCallback] functions. */
    private inline fun <T : Any> PlannerEventCallback?.doEvent(eventName: String, input: Any, crossinline block: () -> T): T {
        if (this == null) return block()
        val startTime = Instant.now()
        return block().also { output ->
            val endTime = Instant.now()
            this(PlannerEvent(eventName, input, output, Duration.between(startTime, endTime)))
        }
    }
}
