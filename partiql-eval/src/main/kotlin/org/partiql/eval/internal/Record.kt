package org.partiql.eval.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class Record(val values: Array<PartiQLValue>) {

    companion object {
        val empty = Record(emptyArray())
        fun of(vararg values: PartiQLValue) = Record(arrayOf(*(values)))
    }
}
