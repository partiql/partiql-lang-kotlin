package org.partiql.value

import org.partiql.spi.utils.NumberExtensions.compareTo
import org.partiql.spi.utils.NumberExtensions.isNaN
import org.partiql.spi.utils.NumberExtensions.isNegInf
import org.partiql.spi.utils.NumberExtensions.isPosInf
import org.partiql.spi.utils.NumberExtensions.isZero

internal class PartiQLValueComparatorInternal(private val nullsFirst: Boolean) : Comparator<PartiQLValue> {
    companion object {
        private const val EQUAL = 0
        private const val LESS = -1
        private const val GREATER = 1
    }

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

    private fun compareInternal(l: PartiQLValue, r: PartiQLValue, nullsFirst: Boolean): Int {
        if (l.withoutAnnotations() == r.withoutAnnotations()) {
            return EQUAL
        }

        when {
            // TODO: This is EQG-specific behavior. Do we want to leave it as-is in the Value comparator?
            l.isNullOrMissing() && r.isNullOrMissing() -> return EQUAL
            l.isNullOrMissing() -> return when (nullsFirst) {
                true -> LESS
                false -> GREATER
            }
            r.isNullOrMissing() -> return when (nullsFirst) {
                true -> GREATER
                false -> LESS
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
        return compareInternal(l, r, nullsFirst)
    }
}
