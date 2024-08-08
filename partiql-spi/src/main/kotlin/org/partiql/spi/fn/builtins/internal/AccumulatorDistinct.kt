package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import java.util.TreeSet

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorDistinct(
    private val _delegate: Accumulator,
) : Accumulator() {

    // TODO: Add support for a datum comparator once the accumulator passes datums instead of PartiQL values.
    @OptIn(PartiQLValueExperimental::class)
    private val seen = TreeSet(PartiQLValue.comparator())

    override fun nextValue(value: Datum) {
        @OptIn(PartiQLValueExperimental::class)
        val pValue = value.toPartiQLValue()
        if (!seen.contains(pValue)) {
            seen.add(pValue)
            _delegate.nextValue(value)
        }
    }

    override fun value(): Datum {
        return _delegate.value()
    }
}
