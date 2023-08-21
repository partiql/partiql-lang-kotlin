package org.partiql.planner

import org.partiql.errors.ProblemCallback
import org.partiql.plan.PartiQLPlan

interface PartiQLPlannerPass {

    fun apply(plan: PartiQLPlan, onProblem: ProblemCallback): PartiQLPlan
}
