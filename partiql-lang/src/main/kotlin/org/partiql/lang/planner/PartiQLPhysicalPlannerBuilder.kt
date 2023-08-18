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
import org.partiql.planner.GlobalVariableResolver
import org.partiql.planner.PlannerEventCallback

/**
 * Builder class to instantiate a [PartiQLPhysicalPlanner].
 */
@ExperimentalPartiQLCompilerPipeline
class PartiQLPhysicalPlannerBuilder private constructor() {

    private var globalVariableResolver = GlobalVariableResolver.EMPTY
    private var physicalPlanPasses: List<PartiQLPhysicalPass> = emptyList()
    private var callback: PlannerEventCallback? = null
    private var options = PartiQLPhysicalPlanner.Options()

    companion object {

        @JvmStatic
        fun standard() = PartiQLPhysicalPlannerBuilder()
    }

    fun globalVariableResolver(globalVariableResolver: GlobalVariableResolver) = this.apply {
        this.globalVariableResolver = globalVariableResolver
    }

    fun physicalPlannerPasses(partiQLPhysicalPassPlanPasses: List<PartiQLPhysicalPass>) = this.apply {
        this.physicalPlanPasses = partiQLPhysicalPassPlanPasses
    }

    fun options(options: PartiQLPhysicalPlanner.Options) = this.apply {
        this.options = options
    }

    fun callback(callback: PlannerEventCallback) = this.apply {
        this.callback = callback
    }

    fun build(): PartiQLPhysicalPlanner = PartiQLPhysicalPlannerDefault(
        globalVariableResolver = globalVariableResolver,
        physicalPlanPasses = physicalPlanPasses,
        callback = callback,
        options = options
    )
}
