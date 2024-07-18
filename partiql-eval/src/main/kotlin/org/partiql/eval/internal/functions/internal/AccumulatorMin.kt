package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorMin : Accumulator() {

    var min: PartiQLValue = nullValue()

    override fun nextValue(value: PartiQLValue) {
        min = comparisonAccumulator(PartiQLValue.comparator(nullsFirst = false))(min, value)
    }

    override fun value(): PartiQLValue = min
}
