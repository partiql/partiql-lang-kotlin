package org.partiql.eval.internal.routines.internal

import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.nullValue


internal class AccumulatorMin : Accumulator() {

    var min: Datum = nullValue()

    override fun nextValue(value: Datum) {
        min = comparisonAccumulator(Datum.comparator(nullsFirst = false))(min, value)
    }

    override fun value(): Datum = min
}
