package org.partiql.eval.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class Record(val values: Array<PartiQLValue>) {

    constructor(vararg values: PartiQLValue) : this(arrayOf(*(values)))

    companion object {
        val empty = Record(emptyArray())
    }
}
