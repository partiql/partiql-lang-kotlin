package org.partiql.eval.internal.routines.internal

import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.nullValue


internal class AccumulatorMax : Accumulator() {

    var max: Datum = nullValue()

    override fun nextValue(value: Datum) {
        max = comparisonAccumulator(Datum.comparator(nullsFirst = true).reversed())(max, value)
    }

    override fun value(): Datum = max
}
