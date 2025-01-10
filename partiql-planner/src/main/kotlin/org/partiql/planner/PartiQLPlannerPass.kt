package org.partiql.planner

import org.partiql.plan.Plan
import org.partiql.spi.Context

/**
 * Interface specifies a pass that can be applied to a [Plan] by the [PartiQLPlanner].
 */
public interface PartiQLPlannerPass {
    /**
     * Applies this pass to the given [Plan] and returns the resulting [Plan].
     *
     * @param plan The [Plan] to apply this pass to.
     * @param ctx The [Context] to use for this pass.
     * @return The resulting [Plan] after applying this pass.
     */
    public fun apply(plan: Plan, ctx: Context): Plan
}
