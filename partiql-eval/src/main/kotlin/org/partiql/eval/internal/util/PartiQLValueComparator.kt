package org.partiql.eval.internal.util

import org.partiql.value.BagValue
import org.partiql.value.BlobValue
import org.partiql.value.BoolValue
import org.partiql.value.ClobValue
import org.partiql.value.DateValue
import org.partiql.value.ListValue
import org.partiql.value.MissingValue
import org.partiql.value.NullValue
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.ScalarValue
import org.partiql.value.SexpValue
import org.partiql.value.StructValue
import org.partiql.value.TextValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue

/**
 * Provides a total, natural ordering over [PartiQLValue] as defined by section 12.2 of the PartiQL specification
 * (https://partiql.org/assets/PartiQL-Specification.pdf#subsection.12.2). PartiQL treats Ion typed nulls as `NULL`
 * for the purposes of comparisons and Ion annotations are not considered for comparison purposes.
 *
 * The ordering rules are as follows:
 *
 *  * [NullValue] and [MissingValue] are always first or last and compare equally.  In other words,
 *    comparison cannot distinguish between `NULL` or `MISSING`.
 *  * The [BoolValue] values follow with `false` coming before `true`.
 *  * The [NumericValue] types come next ordered by their numerical value irrespective
 *    of precision or specific type.
 *      For `FLOAT` special values, `nan` comes before `-inf`, which comes before all normal
 *      numeric values, which is followed by `+inf`.
 *  * [DateValue] values follow and are compared by the date from earliest to latest.
 *  * [TimeValue] values follow and are compared by the time of the day (point of time in a day of 24 hours)
 *      from earliest to latest. Note that time without time zone is not directly comparable with time with time zone.
 *  * [TimestampValue] values follow and are compared by the point of time irrespective of precision and
 *    local UTC offset.
 *  * The [TextValue] types come next ordered by their lexicographical ordering by
 *    Unicode scalar irrespective of their specific type.
 *  * The [BlobValue] and [ClobValue] types follow and are ordered by their lexicographical ordering
 *    by octet.
 *  * [ListValue] comes next, and their values compare lexicographically based on their
 *    child elements recursively based on this definition.
 *  * [SexpValue] follows and compares within its type similar to `LIST`.
 *  * [StructValue] values follow and compare lexicographically based on the *sorted*
 *    (as defined by this definition) members, as pairs of field name and the member value.
 *  * [BagValue] values come finally (except with [NullOrder.LAST]), and their values
 *    compare lexicographically based on the *sorted* child elements.
 *
 *  @param nullOrder that places [NullValue], [MissingValue], and typed Ion null values first or last
 */
@OptIn(PartiQLValueExperimental::class)
internal class PartiQLValueComparator(private val nullOrder: NullOrder) : Comparator<PartiQLValue> {
    /** Whether null values come first or last. */
    enum class NullOrder {
        FIRST,
        LAST
    }

    private val EQUAL = 0
    private val LESS = -1
    private val GREATER = 1

    private fun PartiQLValue.isNullOrMissing(): Boolean = this is NullValue || this is MissingValue || this.isNull
    private fun PartiQLValue.isLob(): Boolean = this is BlobValue || this is ClobValue

    private val structFieldComparator = object : Comparator<Pair<String, PartiQLValue>> {
        override fun compare(left: Pair<String, PartiQLValue>, right: Pair<String, PartiQLValue>): Int {
            val cmpKey = left.first.compareTo(right.first)
            if (cmpKey != 0) {
                return cmpKey
            }
            return compare(left.second, right.second)
        }
    }

    private fun compareInternal(l: PartiQLValue, r: PartiQLValue, nullsFirst: NullOrder): Int {
        if (l.withoutAnnotations() == r.withoutAnnotations()) {
            return EQUAL
        }

        when {
            l.isNullOrMissing() && r.isNullOrMissing() -> return EQUAL
            l.isNullOrMissing() -> return when (nullsFirst) {
                NullOrder.FIRST -> LESS
                NullOrder.LAST -> GREATER
            }
            r.isNullOrMissing() -> return when (nullsFirst) {
                NullOrder.FIRST -> GREATER
                NullOrder.LAST -> LESS
            }
        }

        // BOOL comparator
        when {
            l is BoolValue && r is BoolValue -> {
                val lv = l.value!!
                val rv = r.value!!
                return when {
                    !lv -> LESS
                    else -> GREATER
                }
            }
            l is BoolValue -> return LESS
            r is BoolValue -> return GREATER
        }

        // NUMBER comparator
        when {
            l is NumericValue<*> && r is NumericValue<*> -> {
                val lv = l.value!!
                val rv = r.value!!
                return when {
                    lv.isNaN && rv.isNaN -> EQUAL
                    lv.isNaN -> LESS
                    rv.isNaN -> GREATER
                    lv.isNegInf && rv.isNegInf -> EQUAL
                    lv.isNegInf -> LESS
                    rv.isNegInf -> GREATER
                    lv.isPosInf && rv.isPosInf -> EQUAL
                    lv.isPosInf -> GREATER
                    rv.isPosInf -> LESS
                    lv.isZero() && rv.isZero() -> return EQUAL
                    else -> lv.compareTo(rv)
                }
            }
            l is NumericValue<*> -> return LESS
            r is NumericValue<*> -> return GREATER
        }

        // DATE
        when {
            l is DateValue && r is DateValue -> {
                val lv = l.value!!
                val rv = r.value!!
                return lv.compareTo(rv)
            }
            l is DateValue -> return LESS
            r is DateValue -> return GREATER
        }

        // TIME
        when {
            l is TimeValue && r is TimeValue -> {
                val lv = l.value!!
                val rv = r.value!!
                return lv.compareTo(rv)
            }
            l is TimeValue -> return LESS
            r is TimeValue -> return GREATER
        }

        // TIMESTAMP
        when {
            l is TimestampValue && r is TimestampValue -> {
                val lv = l.value!!
                val rv = r.value!!
                return lv.compareTo(rv)
            }
            l is TimestampValue -> return LESS
            r is TimestampValue -> return GREATER
        }

        // TEXT
        when {
            l is TextValue<*> && r is TextValue<*> -> {
                val lv = l.string!!
                val rv = r.string!!
                return lv.compareTo(rv)
            }
            l is TextValue<*> -> return LESS
            r is TextValue<*> -> return GREATER
        }

        // LOB
        when {
            l.isLob() && r.isLob() -> {
                val lv = ((l as ScalarValue<*>).value) as ByteArray
                val rv = ((r as ScalarValue<*>).value) as ByteArray
                val commonLen = minOf(lv.size, rv.size)
                for (i in 0 until commonLen) {
                    val lOctet = lv[i].toInt() and 0xFF
                    val rOctet = rv[i].toInt() and 0xFF
                    val diff = lOctet - rOctet
                    if (diff != 0) {
                        return diff
                    }
                }
                return lv.size - rv.size
            }
            l.isLob() -> return LESS
            r.isLob() -> return GREATER
        }

        // LIST
        when {
            l is ListValue<*> && r is ListValue<*> -> {
                return compareOrdered(l, r, this)
            }
            l is ListValue<*> -> return LESS
            r is ListValue<*> -> return GREATER
        }

        // SEXP
        when {
            l is SexpValue<*> && r is SexpValue<*> -> {
                return compareOrdered(l, r, this)
            }
            l is SexpValue<*> -> return LESS
            r is SexpValue<*> -> return GREATER
        }

        // STRUCT
        when {
            l is StructValue<*> && r is StructValue<*> -> {
                val entriesL = l.entries
                val entriesR = r.entries
                return compareUnordered(entriesL, entriesR, structFieldComparator)
            }
            l is StructValue<*> -> return LESS
            r is StructValue<*> -> return GREATER
        }

        // BAG
        when {
            l is BagValue<*> && r is BagValue<*> -> {
                return compareUnordered(l, r, this)
            }
            l is BagValue<*> -> return LESS
            r is BagValue<*> -> return GREATER
        }
        throw IllegalStateException("Could not compare: $l and $r")
    }

    private fun <T> compareOrdered(
        l: Iterable<T>,
        r: Iterable<T>,
        elementComparator: Comparator<T>
    ): Int {
        val lIter = l.iterator()
        val rIter = r.iterator()
        while (lIter.hasNext() && rIter.hasNext()) {
            val lVal = lIter.next()
            val rVal = rIter.next()
            val result = elementComparator.compare(lVal, rVal)
            if (result != 0) {
                return result
            }
        }
        return when {
            lIter.hasNext() -> GREATER
            rIter.hasNext() -> LESS
            else -> EQUAL
        }
    }

    private fun <T> compareUnordered(
        l: Iterable<T>,
        r: Iterable<T>,
        elementComparator: Comparator<T>
    ): Int {
        val sortedL = l.sortedWith(elementComparator)
        val sortedR = r.sortedWith(elementComparator)
        return compareOrdered(sortedL, sortedR, elementComparator)
    }

    override fun compare(l: PartiQLValue, r: PartiQLValue): Int {
        return compareInternal(l, r, nullOrder)
    }
}
