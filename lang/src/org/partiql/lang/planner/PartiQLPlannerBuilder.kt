package org.partiql.lang.planner

import com.amazon.ion.IonSystem

class PartiQLPlannerBuilder private constructor(private val ion: IonSystem) {

    private var globalVariableResolver = GlobalVariableResolver.EMPTY
    private val physicalPlanPasses = mutableListOf<PartiQLPlannerPass.Physical>()
    private var callback: PlannerEventCallback? = null
    private var options = PartiQLPlanner.Options()

    companion object {

        @JvmStatic
        fun standard(ion: IonSystem) = PartiQLPlannerBuilder(ion)
    }

    fun globalVariableResolver(g: GlobalVariableResolver) = this.apply {
        globalVariableResolver = g
    }

    fun addPass(pass: PartiQLPlannerPass<*>) = this.apply {
        when (pass) {
            is PartiQLPlannerPass.Physical -> physicalPlanPasses.add(pass)
            else -> error("PartiQLPlanner currently supports only `PartiQLPlannerPass.Physical` planner passes")
        }
    }

    fun options(o: PartiQLPlanner.Options) = this.apply {
        options = o
    }

    fun callback(cb: PlannerEventCallback) = this.apply {
        callback = cb
    }

    fun build(): PartiQLPlanner = PartiQLPlannerImpl(
        globalVariableResolver = globalVariableResolver,
        physicalPlanPasses = physicalPlanPasses,
        callback = callback,
        options = options
    )
}
