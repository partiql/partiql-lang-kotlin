package org.partiql.spi.function.builtins.internal

import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class AccumulatorAnySome : Accumulator() {

    private var res: Datum? = null

    override fun nextValue(value: Datum) {
        checkIsBooleanType("ANY/SOME", value)
        res = res?.let { Datum.bool(it.booleanValue() || value.booleanValue()) } ?: value
    }

    override fun value(): Datum = res ?: Datum.nullValue(PType.bool())
}
