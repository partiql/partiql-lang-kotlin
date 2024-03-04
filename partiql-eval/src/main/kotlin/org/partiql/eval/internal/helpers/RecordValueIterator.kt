package org.partiql.eval.internal.helpers

import org.partiql.eval.internal.Record
import org.partiql.value.CollectionValue
import org.partiql.value.PartiQLValueExperimental

/**
 * An [Iterator] over a [CollectionValue] lazily producing [Record]s as you call [next].
 */
@OptIn(PartiQLValueExperimental::class)
internal class RecordValueIterator(
    collectionValue: CollectionValue<*>
) : Iterator<Record> {

    private val collectionIter = collectionValue.iterator()

    override fun hasNext(): Boolean = collectionIter.hasNext()

    override fun next(): Record = Record(Array(1) { collectionIter.next() })
}
