package org.partiql.spi.fn.builtins.internal

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

    @OptIn(PartiQLValueExperimental::class)
    override fun nextValue(value: PartiQLValue) {
        if (!seen.contains(value)) {
            seen.add(value)
            _delegate.nextValue(value)
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun value(): PartiQLValue {
        return _delegate.value()
    }
}
