package org.partiql.spi.connector.sql.builtins.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.nullValue

@OptIn(PartiQLValueExperimental::class)
internal class AccumulatorEvery : Accumulator() {

    private var res: PartiQLValue? = null

    @OptIn(PartiQLValueExperimental::class)
    override fun nextValue(value: PartiQLValue) {
        checkIsBooleanType("EVERY", value)
        res = res?.let { boolValue(it.booleanValue() && value.booleanValue()) } ?: value
    }

    override fun value(): PartiQLValue = res ?: nullValue()
}
