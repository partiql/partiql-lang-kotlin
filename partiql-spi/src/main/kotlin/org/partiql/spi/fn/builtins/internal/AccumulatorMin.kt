package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum

internal class AccumulatorMin : Accumulator() {

    var min: Datum = Datum.nullValue()
    private val comparator = Datum.comparator(false)

    override fun nextValue(value: Datum) {
        min = comparisonAccumulator(comparator)(min, value)
    }

    override fun value(): Datum = min
}
