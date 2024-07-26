package org.partiql.planner.internal.fn.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int64Value

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorCount : Accumulator() {

    var count: Long = 0L

    override fun nextValue(value: PartiQLValue) {
        this.count += 1L
    }

    override fun value(): PartiQLValue = int64Value(count)
}
