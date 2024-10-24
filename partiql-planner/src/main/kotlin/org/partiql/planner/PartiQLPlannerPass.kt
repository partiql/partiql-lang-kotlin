package org.partiql.planner

import org.partiql.plan.Plan
import org.partiql.spi.Context

public interface PartiQLPlannerPass {

    public fun apply(plan: Plan, ctx: Context): Plan
}
