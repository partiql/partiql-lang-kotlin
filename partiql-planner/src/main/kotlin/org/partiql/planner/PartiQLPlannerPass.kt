package org.partiql.planner

import org.partiql.plan.Plan
import org.partiql.spi.errors.PErrorListener

public interface PartiQLPlannerPass {

    public fun apply(plan: Plan, listener: PErrorListener): Plan
}
