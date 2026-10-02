/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.WindowPartition
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Base class for the window functions that return the value of a row within the current row's window frame:
 * FIRST_VALUE, LAST_VALUE, and NTH_VALUE. See SQL:2011 6.10 General Rules 1.c and 1.d.
 *
 * Window frame: the window framing clause is not yet supported, so the window frame is always the default frame
 * (SQL:2011 7.11 <window clause>):
 *  - with a window ordering clause: RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW, i.e. from the first row of the
 *    partition through the last peer of the current row;
 *  - without a window ordering clause: the entire partition (all rows are peers of each other).
 * Both cases are covered by `[0, orderingGroupEnd]`. The frame bounds are computed solely in [frameStart]/[frameEnd],
 * so that a future window framing implementation only needs to change those.
 *
 * @param value the <value expression> to apply to the rows of the window frame
 * @param ignoreNulls whether IGNORE NULLS was specified
 * @param returnType the declared type of the result; used for the null value that is returned when the result is null
 */
internal abstract class FrameValueFunction(
    private val value: ExprValue,
    private val ignoreNulls: Boolean,
    returnType: PType,
) : OuterScopedWindowFunction {

    private lateinit var _partition: WindowPartition

    /**
     * The window operator's environment, without any row of the partition pushed; see [OuterScopedWindowFunction].
     */
    private lateinit var _outer: Environment

    /**
     * The (lazily computed) values of [value] for rows `0 until _values.size` of the partition.
     */
    private val _values: MutableList<Datum> = mutableListOf()

    /**
     * The (sorted) row indexes, among the rows `0 until _values.size`, whose value is neither NULL nor MISSING.
     * Only maintained when [ignoreNulls] is true.
     */
    private val _nonNullIndexes: MutableList<Long> = mutableListOf()

    /**
     * The null value of the declared result type.
     */
    protected val nullResult: Datum = when (returnType.code()) {
        PType.DYNAMIC, PType.UNKNOWN, PType.VARIANT -> Datum.nullValue()
        else -> Datum.nullValue(returnType)
    }

    /**
     * Returns the result of the window function for the current row.
     * @param env the environment of the current row
     * @param frameStart the (inclusive) index of the first row of the window frame
     * @param frameEnd the (inclusive) index of the last row of the window frame; the frame is empty if less than [frameStart]
     */
    protected abstract fun evalFrame(env: Environment, frameStart: Long, frameEnd: Long): Datum

    override fun setOuterEnvironment(env: Environment) {
        _outer = env
    }

    override fun reset(partition: WindowPartition) {
        _partition = partition
        _values.clear()
        _nonNullIndexes.clear()
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        val start = frameStart(orderingGroupStart, orderingGroupEnd)
        val end = frameEnd(orderingGroupStart, orderingGroupEnd)
        return evalFrame(env, start, end)
    }

    /**
     * Default frame (no window framing clause): UNBOUNDED PRECEDING.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun frameStart(orderingGroupStart: Long, orderingGroupEnd: Long): Long = 0L

    /**
     * Default frame (no window framing clause): CURRENT ROW in RANGE mode, i.e. the last peer of the current row.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun frameEnd(orderingGroupStart: Long, orderingGroupEnd: Long): Long = orderingGroupEnd

    /**
     * Returns the [n]-th value of TX, where TX is the sequence of values that is the result of applying [value] to
     * each row of the window frame `[frameStart, frameEnd]` (eliminating the null values if IGNORE NULLS is specified).
     * @param n the 1-based position within TX; must be greater than 0
     * @param fromLast if true, the position is counted from the end of TX (i.e. the (TXN - n + 1)-th value of TX)
     * @return the value; the null value if TX contains fewer than [n] values
     */
    protected fun nthValue(frameStart: Long, frameEnd: Long, n: Long, fromLast: Boolean): Datum {
        val start = maxOf(frameStart, 0L)
        val end = minOf(frameEnd, _partition.size() - 1)
        if (start > end) {
            return nullResult
        }
        load(end)
        if (!ignoreNulls) {
            // TXN = end - start + 1; return null if TXN < n
            if (n > end - start + 1) {
                return nullResult
            }
            val index = if (fromLast) end - (n - 1) else start + (n - 1)
            return _values[index.toInt()]
        }
        // Find the non-null values within [start, end]: _nonNullIndexes[lo until hi]
        val lo = lowerBound(start)
        val hi = lowerBound(end + 1)
        val txn = (hi - lo).toLong()
        if (n > txn) {
            return nullResult
        }
        val position = if (fromLast) hi - n.toInt() else lo + n.toInt() - 1
        return _values[_nonNullIndexes[position].toInt()]
    }

    /**
     * Evaluates [value] for each row of the partition up to (and including) [index], if not already evaluated. The rows
     * are pushed onto the outer environment rather than onto the current row's environment; see
     * [OuterScopedWindowFunction].
     */
    private fun load(index: Long) {
        while (_values.size <= index) {
            val i = _values.size.toLong()
            val row = _partition[i]
            val datum = value.eval(_outer.push(row))
            _values.add(datum)
            if (ignoreNulls && !datum.isNull && !datum.isMissing) {
                _nonNullIndexes.add(i)
            }
        }
    }

    /**
     * @return the position of the first element of [_nonNullIndexes] that is greater than or equal to [index].
     */
    private fun lowerBound(index: Long): Int {
        var lo = 0
        var hi = _nonNullIndexes.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (_nonNullIndexes[mid] < index) lo = mid + 1 else hi = mid
        }
        return lo
    }
}
