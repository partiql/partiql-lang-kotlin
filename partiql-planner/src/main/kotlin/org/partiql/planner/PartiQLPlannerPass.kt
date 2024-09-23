package org.partiql.planner

import org.partiql.errors.ProblemCallback
import org.partiql.plan.v1.PartiQLPlan

public interface PartiQLPlannerPass {

    public fun apply(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan
}
