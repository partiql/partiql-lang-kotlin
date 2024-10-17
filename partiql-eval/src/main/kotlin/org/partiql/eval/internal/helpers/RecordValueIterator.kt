package org.partiql.eval.internal.helpers

import org.partiql.eval.Row
import org.partiql.spi.value.Datum

/**
 * An [Iterator] over an [Iterator] lazily producing [Row]s as you call [next].
 */
internal class RecordValueIterator(
    collectionValue: Iterator<Datum>
) : Iterator<Row> {

    private val collectionIter = collectionValue.iterator()

    override fun hasNext(): Boolean = collectionIter.hasNext()

    override fun next(): Row =
        Row(Array(1) { collectionIter.next() })
}
