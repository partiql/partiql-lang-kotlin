package org.partiql.eval.internal.helpers

import org.partiql.eval.Row
import org.partiql.spi.value.Datum

internal object DatumArrayComparator : Comparator<Row> {
    private val delegate = Datum.comparator(false)
    override fun compare(o1: Row, o2: Row): Int {
        val o1Size = o1.size
        val o2Size = o2.size
        if (o1Size < o2Size) {
            return -1
        }
        if (o1Size > o2Size) {
            return 1
        }
        for (index in 0 until o2.size) {
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
