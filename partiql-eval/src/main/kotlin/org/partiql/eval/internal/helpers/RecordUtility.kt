package org.partiql.eval.internal.helpers

import org.partiql.eval.value.Datum

internal object RecordUtility {
    /**
     * Coerces missing values into null values. Currently used when the [Datum.comparator] is used in a TreeSet/TreeMap
     * (treats null and missing as the same value) and we need to deterministically return a value. Here we use coerce
     * to null to follow the PartiQL spec's grouping function.
     */
    fun Array<Datum>.coerceMissing() {
        for (i in indices) {
            if (this[i].isMissing) {
                this[i] = Datum.nullValue()
            }
        }
    }
}
