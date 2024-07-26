package org.partiql.planner.internal.fn.sql.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.nullValue

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorAnySome : Accumulator() {

    private var res: PartiQLValue? = null

    override fun nextValue(value: PartiQLValue) {
        checkIsBooleanType("ANY/SOME", value)
        res = res?.let { boolValue(it.booleanValue() || value.booleanValue()) } ?: value
    }

    override fun value(): PartiQLValue = res ?: nullValue()
}
