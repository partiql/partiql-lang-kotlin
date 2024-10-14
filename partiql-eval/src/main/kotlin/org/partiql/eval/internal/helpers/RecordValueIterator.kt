package org.partiql.eval.internal.helpers

import org.partiql.eval.operator.Record
import org.partiql.spi.value.Datum

/**
 * An [Iterator] over an [Iterator] lazily producing [Record]s as you call [next].
 */
internal class RecordValueIterator(
    collectionValue: Iterator<Datum>
) : Iterator<Record> {

    private val collectionIter = collectionValue.iterator()

    override fun hasNext(): Boolean = collectionIter.hasNext()

    override fun next(): Record = Record(Array(1) { collectionIter.next() })
}
