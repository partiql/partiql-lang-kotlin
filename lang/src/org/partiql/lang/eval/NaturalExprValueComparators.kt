/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import org.partiql.lang.eval.ExprValueType.*
import org.partiql.lang.util.*

/**
 * Provides a total, natural ordering over [ExprValue].  This ordering is consistent with
 * [ExprValue.exprEquals] with the exception that `NULL` and `MISSING` compare with themselves
 * and have order.  PartiQL treats Ion typed nulls as `NULL` for the purposes of comparisons
 * and Ion annotations are not considered for comparison purposes.
 *
 * The ordering rules are as follows:
 *
 *  * `NULL` and `MISSING` are always first or last and compare equally.  In other words,
 *    comparison cannot distinguish between `NULL` or `MISSING`.
 *  * The `BOOL` values follow with `false` coming before `true`.
 *  * The [ExprValueType.isNumber] types come next ordered by their numerical value irrespective
 *    of precision or specific type.
 *      For `FLOAT` special values, `nan` comes before `-inf`, which comes before all normal
 *      numeric values, which is followed by `+inf`.
 *  * `DATE` values follow and are compared by the date from earliest to latest.
 *  * `TIME` values follow and are compared by the time of the day (point of time in a day of 24 hours)
 *      from earliest to latest. Note that time without time zone is not directly comparable with time with time zone.
 *      However, time without time zone value comes before time with time zone value when compared in the natural order.
 *  * `TIMESTAMP` values follow and are compared by the point of time irrespective of precision and
 *    local UTC offset.
 *  * The [ExprValueType.isText] types come next ordered by their lexicographical ordering by
 *    Unicode scalar irrespective of their specific type.
 *  * The [ExprValueType.isLob] types follow and are ordered by their lexicographical ordering
 *    by octet.
 *  * `LIST` comes next, and their values compare lexicographically based on their
 *    child elements recursively based on this definition.
 *  * `SEXP` follows and compares within its type similar to `LIST`.
 *  * `STRUCT` values follow and compare lexicographically based on the *sorted*
 *    (as defined by this definition) members, as pairs of field name and the member value.
 *  * `BAG` values come finally (except with [NullOrder.NULLS_LAST]), and their values
 *    compare lexicographically based on the *sorted* child elements.
 */
enum class NaturalExprValueComparators(private val nullOrder: NullOrder) : Comparator<ExprValue> {
    NULLS_FIRST(NullOrder.FIRST),
    NULLS_LAST(NullOrder.LAST);

    /** Whether or not null values come first or last. */
    private enum class NullOrder {
        FIRST,
        LAST
    }

    companion object {
        private const val EQUAL = 0
        private const val LESS = -1
        private const val MORE = 1
    }

    /**
     * Generalizes the type handling predicates in the comparator.
     * The premise is that this is used in the order of ascending types so if
     * the left type is the specified condition and the right type isn't this implies
     * that the left value is less than the right and vice versa.
     */
    private inline fun handle(leftTypeCond: Boolean,
                              rightTypeCond: Boolean,
                              sameTypeHandler: () -> Int): Int? = when {

        leftTypeCond && rightTypeCond -> sameTypeHandler()
        leftTypeCond -> LESS
        rightTypeCond -> MORE
        else -> null
    }

    private inline fun ifCompared(value: Int?, handler: (Int) -> Unit): Unit {
        if (value != null) {
            handler(value)
        }
    }

    private fun <T> compareOrdered(left: Iterable<T>,
                                   right: Iterable<T>,
                                   comparator: Comparator<T>): Int {
        val lIter = left.iterator()
        val rIter = right.iterator()

        while (lIter.hasNext() && rIter.hasNext()) {
            val lChild = lIter.next()
            val rChild = rIter.next()
            val cmp = comparator.compare(lChild, rChild)
            if (cmp != 0) {
                return cmp
            }
        }

        return when {
            lIter.hasNext() -> MORE
            rIter.hasNext() -> LESS
            else -> EQUAL
        }
    }

    private fun <T> compareUnordered(left: Iterable<T>,
                                     right: Iterable<T>,
                                     entityCmp: Comparator<T>): Int {
        val pairCmp = object : Comparator<Pair<T, Int>> {
            override fun compare(o1: Pair<T, Int>, o2: Pair<T, Int>): Int {
                val cmp = entityCmp.compare(o1.first, o2.first)
                if (cmp != 0) {
                    return cmp
                }
                return o2.second - o1.second
            }
        }

        fun Iterable<T>.sorted(): Iterable<T> =
            this.mapIndexed { i, e -> Pair(e, i) }
                .toSortedSet(pairCmp)
                .asSequence()
                .map { (e, _) -> e }
                .asIterable()

        return compareOrdered(left.sorted(), right.sorted(), entityCmp)
    }

    private val structFieldComparator = object : Comparator<ExprValue> {
        override fun compare(left: ExprValue, right: ExprValue): Int {
            val lName = left.name ?: errNoContext("Internal error: left struct field has no name", internal = true)
            val rName = right.name ?: errNoContext("Internal error: left struct field has no name", internal = true)
            val cmp = this@NaturalExprValueComparators.compare(lName, rName)
            if (cmp != 0) {
                return cmp
            }

            return this@NaturalExprValueComparators.compare(left, right)
        }
    }

    override fun compare(left: ExprValue, right: ExprValue): Int {
        if (left === right) return EQUAL

        val lType = left.type
        val rType = right.type

        if (nullOrder == NullOrder.FIRST) {
            ifCompared(handle(lType.isUnknown, rType.isUnknown) { EQUAL }) { return it }
        }

        // Bool
        ifCompared(
            handle(lType == BOOL, rType == BOOL) {
                val lVal = left.booleanValue()
                val rVal = right.booleanValue()

                when {
                    lVal == rVal -> EQUAL
                    !lVal -> LESS
                    else -> MORE
                }
            }
        ) { return it }

        // Numbers
        ifCompared(
            handle(lType.isNumber, rType.isNumber) {
                val lVal = left.numberValue()
                val rVal = right.numberValue()

                ifCompared(handle(lVal.isNaN, rVal.isNaN) { EQUAL }) { return it }
                ifCompared(handle(lVal.isNegInf, rVal.isNegInf) { EQUAL }) { return it }
                // +inf gets handled in a slightly reverse way than the normal pattern
                // because it comes after every other value
                when {
                    lVal.isPosInf && rVal.isPosInf -> return EQUAL
                    lVal.isPosInf -> return MORE
                    rVal.isPosInf -> return LESS
                    lVal.isZero() && rVal.isZero() -> return EQUAL // for negative zero
                    else -> return lVal.compareTo(rVal)
                }
            }
        ) { return it }

        // Date
        ifCompared(
            handle(lType == DATE, rType == DATE) {
                val lVal = left.dateValue()
                val rVal = right.dateValue()

                return lVal.compareTo(rVal)
            }
        ) { return it }

        // Time
        ifCompared(
            handle(lType == TIME, rType == TIME) {
                val lVal = left.timeValue()
                val rVal = right.timeValue()

                return lVal.naturalOrderCompareTo(rVal)
            }
        ) { return it }

        // Timestamp
        ifCompared(
            handle(lType == TIMESTAMP, rType == TIMESTAMP) {
                val lVal = left.timestampValue()
                val rVal = right.timestampValue()

                return lVal.compareTo(rVal)
            }
        ) { return it }

        // Text
        ifCompared(
            handle(lType.isText, rType.isText) {
                val lVal = left.stringValue()
                val rVal = right.stringValue()

                return lVal.compareTo(rVal)
            }
        ) { return it }

        // LOB
        ifCompared(
            handle(lType.isLob, rType.isLob) {
                val lVal = left.bytesValue()
                val rVal = right.bytesValue()

                val commonLen = minOf(lVal.size, rVal.size)
                for (i in 0 until commonLen) {
                    val lOctet = lVal[i].toInt() and 0xFF
                    val rOctet = rVal[i].toInt() and 0xFF
                    val diff = lOctet - rOctet
                    if (diff != 0) {
                        return diff
                    }
                }
                return lVal.size - rVal.size
            }
        ) { return it }

        // List
        ifCompared(
            handle(lType == LIST, rType == LIST) {
                return compareOrdered(left, right, this)
            }
        ) { return it }

        // Sexp
        ifCompared(
            handle(lType == SEXP, rType == SEXP) {
                return compareOrdered(left, right, this)
            }
        ) { return it }

        // Struct
        ifCompared(
            handle(lType == STRUCT, rType == STRUCT) {
                compareUnordered(left, right, structFieldComparator)
            }
        ) { return it }

        // Bag
        ifCompared(
            handle(lType == BAG, rType == BAG) {
                compareUnordered(left, right, this)
            }
        ) { return it }

        if (nullOrder == NullOrder.LAST) {
            ifCompared(handle(lType.isUnknown, rType.isUnknown) { EQUAL }) { return it }
        }

        throw IllegalStateException("Could not compare: $left and $right")
    }
}
