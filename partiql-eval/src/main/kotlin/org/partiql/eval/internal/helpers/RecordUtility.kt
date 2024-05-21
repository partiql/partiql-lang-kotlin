package org.partiql.eval.internal.helpers

import org.partiql.eval.internal.Record
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal object RecordUtility {
    @OptIn(PartiQLValueExperimental::class)
    fun Record.toPartiQLValueList(): List<PartiQLValue> = List(this.values.size) {
        this.values[it].toPartiQLValue()
    }
}
