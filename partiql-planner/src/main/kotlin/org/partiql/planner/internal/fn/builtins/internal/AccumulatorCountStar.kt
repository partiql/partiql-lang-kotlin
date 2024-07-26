package org.partiql.planner.internal.fn.builtins.internal

import org.partiql.planner.internal.fn.Agg
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int64Value

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorCountStar : Agg.Accumulator {

    var count: Long = 0L

    override fun next(args: Array<PartiQLValue>) {
        this.count += 1L
    }

    override fun value(): PartiQLValue = int64Value(count)
}
