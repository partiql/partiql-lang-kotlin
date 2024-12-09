package org.partiql.planner.builder

import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerPass
import org.partiql.planner.internal.PlannerFlag
import org.partiql.planner.internal.SqlPlanner

/**
 * PartiQLPlannerBuilder is used to programmatically construct a [PartiQLPlanner] implementation.
 *
 * Usage:
 *      PartiQLPlanner.builder()
 *                    .signal()
 *                    .addPass(myPass)
 *                    .build()
 */
public class PartiQLPlannerBuilder {

    private val flags: MutableSet<PlannerFlag> = mutableSetOf()
    private val passes: MutableList<PartiQLPlannerPass> = mutableListOf()

    /**
     * Build the builder, return an implementation of a [PartiQLPlanner].
     */
    public fun build(): PartiQLPlanner {
        return SqlPlanner(passes, flags)
    }

    /**
     * Java style method for adding a planner pass to this planner builder.
     *
     * @param pass
     * @return
     */
    public fun addPass(pass: PartiQLPlannerPass): PartiQLPlannerBuilder {
        this.passes.add(pass)
        return this
    }

    /**
     * Kotlin style method for adding a list of planner passes to this planner builder.
     *
     * @param passes
     * @return
     */
    public fun addPasses(vararg passes: PartiQLPlannerPass): PartiQLPlannerBuilder {
        this.passes.addAll(passes)
        return this
    }

    /**
     * Java style method for setting the planner to signal mode
     */
    public fun signal(signal: Boolean = true): PartiQLPlannerBuilder {
        if (signal) {
            flags.add(PlannerFlag.SIGNAL_MODE)
        } else {
            flags.remove(PlannerFlag.SIGNAL_MODE)
        }
        return this
    }
}
