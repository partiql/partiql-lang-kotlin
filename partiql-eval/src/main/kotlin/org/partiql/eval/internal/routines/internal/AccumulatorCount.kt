package org.partiql.eval.internal.routines.internal

import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.int64Value


internal class AccumulatorCount : Accumulator() {

    var count: Long = 0L

    override fun nextValue(value: Datum) {
        this.count += 1L
    }

    override fun value(): Datum = int64Value(count)
}
