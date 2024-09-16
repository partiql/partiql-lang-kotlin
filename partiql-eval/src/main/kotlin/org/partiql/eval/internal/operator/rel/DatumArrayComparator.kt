package org.partiql.eval.internal.operator.rel

import org.partiql.spi.value.Datum

internal object DatumArrayComparator : Comparator<Array<Datum>> {
    private val delegate = Datum.comparator(false)
    override fun compare(o1: Array<Datum>, o2: Array<Datum>): Int {
        if (o1.size < o2.size) {
            return -1
        }
        if (o1.size > o2.size) {
            return 1
        }
        for (index in 0..o2.lastIndex) {
            val element1 = o1[index]
            val element2 = o2[index]
            val compared = delegate.compare(element1, element2)
            if (compared != 0) {
                return compared
            }
        }
        return 0
    }
}
