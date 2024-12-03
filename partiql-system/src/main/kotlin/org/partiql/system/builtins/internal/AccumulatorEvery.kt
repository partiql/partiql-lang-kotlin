package org.partiql.system.builtins.internal

import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class AccumulatorEvery : Accumulator() {

    private var res: Datum? = null

    override fun nextValue(value: Datum) {
        checkIsBooleanType("EVERY", value)
        res = res?.let { Datum.bool(it.boolean && value.boolean) } ?: value
    }

    override fun value(): Datum = res ?: Datum.nullValue(PType.bool())
}
