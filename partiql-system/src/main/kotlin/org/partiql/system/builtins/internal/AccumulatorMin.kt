package org.partiql.system.builtins.internal

import org.partiql.spi.value.Datum

internal class AccumulatorMin : Accumulator() {

    var min: Datum = Datum.nullValue()
    private val comparator = Datum.comparator(false)

    override fun nextValue(value: Datum) {
        min = comparisonAccumulator(comparator)(min, value)
    }

    override fun value(): Datum = min
}
