package org.partiql.spi.fn.builtins.internal

import org.partiql.spi.value.Datum

internal class AccumulatorGroupAs : Accumulator() {

    val values = mutableListOf<Datum>()

    override fun nextValue(value: Datum) {
        values.add(value)
    }

    override fun value(): Datum = Datum.bag(values)
}
