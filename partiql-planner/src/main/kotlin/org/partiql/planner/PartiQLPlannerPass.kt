package org.partiql.planner

import org.partiql.plan.PartiQLPlan
import org.partiql.planner.errors.PartiQLPlannerErrorHandler

interface PartiQLPlannerPass {

    public val errorHandler: PartiQLPlannerErrorHandler

    fun apply(plan: PartiQLPlan): PartiQLPlan
}
