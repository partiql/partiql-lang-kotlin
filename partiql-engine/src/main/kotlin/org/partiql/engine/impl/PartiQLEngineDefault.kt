package org.partiql.engine.impl

import org.partiql.engine.PartiQLEngine
import org.partiql.plan.PartiQLPlan
import org.partiql.value.PartiQLValueExperimental

internal class PartiQLEngineDefault : PartiQLEngine {
    @OptIn(PartiQLValueExperimental::class)
    override fun execute(plan: PartiQLPlan): PartiQLEngine.Result {
        val expression = PlanToPhysical.convert(plan)
        val value = expression.evaluate(Record(emptyList()))
        return PartiQLEngine.Result.Success(value)
    }
}
