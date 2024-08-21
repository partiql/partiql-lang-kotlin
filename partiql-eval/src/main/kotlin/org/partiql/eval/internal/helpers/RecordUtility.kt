package org.partiql.eval.internal.helpers

import org.partiql.eval.internal.Record
import org.partiql.eval.value.Datum

internal object RecordUtility {
    /**
     * Converts the [Record] into an array of Datum, while coercing any missing values into null values.
     */
    fun Record.toDatumArrayCoerceMissing(): Array<Datum> = Array(this.values.size) {
        val d = this@toDatumArrayCoerceMissing.values[it]
        when (d.isMissing) {
            true -> Datum.nullValue()
            else -> d
        }
    }
}
