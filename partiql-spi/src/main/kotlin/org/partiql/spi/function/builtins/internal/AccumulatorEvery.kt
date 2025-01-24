package org.partiql.spi.function.builtins.internal

import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils.checkIsBooleanType
import org.partiql.spi.value.Datum

internal class AccumulatorEvery : Accumulator() {

    private var res: Datum? = null

    override fun nextValue(value: Datum) {
        checkIsBooleanType("EVERY", value)
        res = res?.let { Datum.bool(it.boolean && value.boolean) } ?: value
    }

    override fun value(): Datum = res ?: Datum.nullValue(PType.bool())
}
