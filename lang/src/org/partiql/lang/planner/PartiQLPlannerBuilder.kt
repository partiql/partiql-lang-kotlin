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

import OTS.IMP.org.partiql.ots.legacy.plugin.LegacyPlugin
import OTS.ITF.org.partiql.ots.Plugin

/**
 * Builder class to instantiate a [PartiQLPlanner].
 */
class PartiQLPlannerBuilder private constructor() {

    private var globalVariableResolver = GlobalVariableResolver.EMPTY
    private var physicalPlanPasses: List<PartiQLPlannerPass.Physical> = emptyList()
    private var callback: PlannerEventCallback? = null
    private var options = PartiQLPlanner.Options()
    private var plugin: Plugin = LegacyPlugin()

    companion object {

        @JvmStatic
        fun standard() = PartiQLPlannerBuilder()
    }

    fun globalVariableResolver(globalVariableResolver: GlobalVariableResolver) = this.apply {
        this.globalVariableResolver = globalVariableResolver
    }

    fun physicalPlannerPasses(physicalPlanPasses: List<PartiQLPlannerPass.Physical>) = this.apply {
        this.physicalPlanPasses = physicalPlanPasses
    }

    fun options(options: PartiQLPlanner.Options) = this.apply {
        this.options = options
    }

    fun callback(callback: PlannerEventCallback) = this.apply {
        this.callback = callback
    }

    fun plugin(plugin: Plugin) = this.apply {
        this.plugin = plugin
    }

    fun build(): PartiQLPlanner = PartiQLPlannerDefault(
        globalVariableResolver = globalVariableResolver,
        physicalPlanPasses = physicalPlanPasses,
        callback = callback,
        options = options,
        plugin = plugin
    )
}
