package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum
import java.util.TreeSet

internal class AccumulatorDistinct(
    private val _delegate: Accumulator,
) : Accumulator() {

    private val seen = TreeSet(Datum.comparator())

    override fun nextValue(value: Datum) {
        if (!seen.contains(value)) {
            seen.add(value)
            _delegate.nextValue(value)
        }
    }

    override fun value(): Datum {
        return _delegate.value()
    }
}
