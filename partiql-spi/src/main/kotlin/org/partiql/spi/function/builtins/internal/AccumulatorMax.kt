package org.partiql.spi.function.builtins.internal

import org.partiql.spi.utils.FunctionUtils.comparisonAccumulator
import org.partiql.spi.value.Datum

internal class AccumulatorMax : Accumulator() {

    var max: Datum = Datum.nullValue()
    private val comparator = Datum.comparator(true).reversed()

    override fun nextValue(value: Datum) {
        max = comparisonAccumulator(comparator)(max, value)
    }

    override fun value(): Datum = max
}
