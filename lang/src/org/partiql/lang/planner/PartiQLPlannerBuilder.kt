package org.partiql.lang.planner

class PartiQLPlannerBuilder private constructor() {

    private var globalVariableResolver = GlobalVariableResolver.EMPTY
    private var physicalPlanPasses: List<PartiQLPlannerPass.Physical> = emptyList()
    private var callback: PlannerEventCallback? = null
    private var options = PartiQLPlanner.Options()

    companion object {

        @JvmStatic
        fun standard() = PartiQLPlannerBuilder()
    }

    fun withGlobalVariableResolver(globalVariableResolver: GlobalVariableResolver) = this.apply {
        this.globalVariableResolver = globalVariableResolver
    }

    fun withPhysicalPlannerPasses(physicalPlanPasses: List<PartiQLPlannerPass.Physical>) = this.apply {
        this.physicalPlanPasses = physicalPlanPasses
    }

    fun withOptions(options: PartiQLPlanner.Options) = this.apply {
        this.options = options
    }

    fun withCallback(callback: PlannerEventCallback) = this.apply {
        this.callback = callback
    }

    fun build(): PartiQLPlanner = PartiQLPlannerDefault(
        globalVariableResolver = globalVariableResolver,
        physicalPlanPasses = physicalPlanPasses,
        callback = callback,
        options = options
    )
}
