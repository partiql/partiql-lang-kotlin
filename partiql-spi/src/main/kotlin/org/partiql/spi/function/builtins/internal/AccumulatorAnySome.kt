package org.partiql.spi.function.builtins.internal

import org.partiql.eval.value.Datum
import org.partiql.types.PType

internal class AccumulatorAnySome : Accumulator() {

    private var res: Datum? = null

    override fun nextValue(value: Datum) {
        checkIsBooleanType("ANY/SOME", value)
        res = res?.let { Datum.bool(it.booleanValue() || value.booleanValue()) } ?: value
    }

    override fun value(): Datum = res ?: Datum.nullValue(PType.bool())
}
