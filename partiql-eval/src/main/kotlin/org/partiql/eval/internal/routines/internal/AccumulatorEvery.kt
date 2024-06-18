package org.partiql.eval.internal.routines.internal

import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.boolValue
import org.partiql.value.nullValue


internal class AccumulatorEvery : Accumulator() {

    private var res: Datum? = null

    
    override fun nextValue(value: Datum) {
        checkIsBooleanType("EVERY", value)
        res = res?.let { boolValue(it.booleanValue() && value.booleanValue()) } ?: value
    }

    override fun value(): Datum = res ?: nullValue()
}
