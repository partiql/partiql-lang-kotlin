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

import org.partiql.annotation.PartiQLExperimental

/**
 * Builder class to instantiate a [PartiQLPlanner].
 */
@PartiQLExperimental
class PartiQLPlannerBuilder private constructor() {

    private var globalVariableResolver = GlobalVariableResolver.EMPTY
    private var physicalPlanPasses: List<PartiQLPhysicalPass> = emptyList()
    private var callback: PlannerEventCallback? = null
    private var options = PartiQLPlanner.Options()

    companion object {

        @JvmStatic
        fun standard() = PartiQLPlannerBuilder()
    }

    fun globalVariableResolver(globalVariableResolver: GlobalVariableResolver) = this.apply {
        this.globalVariableResolver = globalVariableResolver
    }

    fun physicalPlannerPasses(partiQLPhysicalPassPlanPasses: List<PartiQLPhysicalPass>) = this.apply {
        this.physicalPlanPasses = partiQLPhysicalPassPlanPasses
    }

    fun options(options: PartiQLPlanner.Options) = this.apply {
        this.options = options
    }

    fun callback(callback: PlannerEventCallback) = this.apply {
        this.callback = callback
    }

    fun build(): PartiQLPlanner = PartiQLPlannerDefault(
        globalVariableResolver = globalVariableResolver,
        physicalPlanPasses = physicalPlanPasses,
        callback = callback,
        options = options
    )
}
