package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum

internal class AccumulatorMax : Accumulator() {

    var max: Datum = Datum.nullValue()
    private val comparator = Datum.comparator(true).reversed()

    override fun nextValue(value: Datum) {
        max = comparisonAccumulator(comparator)(max, value)
    }

    override fun value(): Datum = max
}
