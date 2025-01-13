package org.partiql.eval.internal.helpers

import org.partiql.eval.Row
import org.partiql.spi.value.Datum

internal object RecordUtility {
    /**
     * Coerces missing values into null values. Currently used when the [Datum.comparator] is used in a TreeSet/TreeMap
     * (treats null and missing as the same value) and we need to deterministically return a value. Here we use coerce
     * to null to follow the PartiQL spec's grouping function.
     */
    fun Row.coerceMissing() {
        for (i in 0..getSize()) {
            if (this[i].isMissing) {
                this[i] = Datum.nullValue()
            }
        }
    }
}
