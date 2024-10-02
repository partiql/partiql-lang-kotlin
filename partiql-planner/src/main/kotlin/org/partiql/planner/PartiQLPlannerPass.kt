package org.partiql.planner

import org.partiql.errors.ProblemCallback

public interface PartiQLPlannerPass {

    public fun apply(plan: org.partiql.plan.Plan, onProblem: ProblemCallback): org.partiql.plan.Plan
}
