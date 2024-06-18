package org.partiql.eval.internal.routines.internal

import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.bagValue


internal class AccumulatorGroupAs : Accumulator() {

    val values = mutableListOf<Datum>()

    override fun nextValue(value: Datum) {
        values.add(value)
    }

    override fun value(): Datum = bagValue(values)
}
