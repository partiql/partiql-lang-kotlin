package org.partiql.eval.internal.helpers

import org.partiql.errors.TypeCheckException
import org.partiql.eval.value.Datum
import org.partiql.types.PType

/**
 * Coercion function F for bag operators described in RFC-0007.
 *  - F(absent_value) -> << >>
 *  - F(scalar_value) -> << scalar_value >> # singleton bag
 *  - F(tuple_value)  -> << tuple_value >>  # singleton bag, see future extensions
 *  - F(array_value)  -> bag_value          # discard ordering
 *  - F(bag_value)    -> bag_value          # identity
 *
 * Throws a [TypeCheckException] when coercing a non-collection to a bag.
 */
internal fun Datum.asIterator(coerce: Boolean): Iterator<Datum> {
    val d = this
    return if (d.isNull || d.isMissing) {
        coerce.throwOrIterator(emptyList<Datum>().iterator())
    } else {
        when (d.type.kind) {
            PType.Kind.LIST, PType.Kind.BAG, PType.Kind.SEXP -> d.iterator()
            else -> coerce.throwOrIterator(listOf(d).iterator())
        }
    }
}

private fun Boolean.throwOrIterator(iterator: Iterator<Datum>): Iterator<Datum> {
    if (this) {
        return iterator
    } else {
        throw TypeCheckException()
    }
}
