package org.partiql.planner

import org.partiql.plan.PartiQLPlan
import org.partiql.planner.errors.PartiQLPlannerErrorHandler

interface PartiQLPlannerPass {

    fun apply(plan: PartiQLPlan, errorHandler: PartiQLPlannerErrorHandler): PartiQLPlan
}
