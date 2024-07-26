package org.partiql.planner.internal.fn.sql.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorGroupAs : Accumulator() {

    val values = mutableListOf<PartiQLValue>()

    override fun nextValue(value: PartiQLValue) {
        values.add(value)
    }

    override fun value(): PartiQLValue = bagValue(values)
}
