package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorMax : Accumulator() {

    var max: PartiQLValue = nullValue()

    override fun nextValue(value: PartiQLValue) {
        max = comparisonAccumulator(PartiQLValue.comparator(nullsFirst = true).reversed())(max, value)
    }

    override fun value(): PartiQLValue = max
}
