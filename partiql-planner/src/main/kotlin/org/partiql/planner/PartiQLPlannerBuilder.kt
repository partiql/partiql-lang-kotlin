package org.partiql.planner

import org.partiql.planner.internal.PartiQLPlannerDefault
import org.partiql.planner.internal.PlannerFlag

/**
 * PartiQLPlannerBuilder is used to programmatically construct a [PartiQLPlanner] implementation.
 *
 * Usage:
 *      PartiQLPlanner.builder()
 *                    .signalMode()
 *                    .addPass(myPass)
 *                    .build()
 */
public class PartiQLPlannerBuilder {

    private val flags: MutableSet<PlannerFlag> = mutableSetOf()

    private val passes: MutableList<PartiQLPlannerPass> = mutableListOf()

    /**
     * Build the builder, return an implementation of a [PartiQLPlanner].
     *
     * @return
     */
    public fun build(): PartiQLPlanner = PartiQLPlannerDefault(passes, flags)

    /**
     * Java style method for adding a planner pass to this planner builder.
     *
     * @param pass
     * @return
     */
    public fun addPass(pass: PartiQLPlannerPass): PartiQLPlannerBuilder = this.apply {
        this.passes.add(pass)
    }

    /**
     * Kotlin style method for adding a list of planner passes to this planner builder.
     *
     * @param passes
     * @return
     */
    public fun addPasses(vararg passes: PartiQLPlannerPass): PartiQLPlannerBuilder = this.apply {
        this.passes.addAll(passes)
    }

    /**
     * Java style method for setting the planner to signal mode
     */
    public fun signalMode(): PartiQLPlannerBuilder = this.apply {
        this.flags.add(PlannerFlag.SIGNAL_MODE)
    }
}
