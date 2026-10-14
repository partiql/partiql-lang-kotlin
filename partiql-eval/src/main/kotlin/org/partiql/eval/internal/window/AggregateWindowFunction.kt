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
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.eval.internal.helpers.checkInterrupted
import org.partiql.eval.internal.operator.Aggregate
import org.partiql.spi.value.Datum

/**
 * An aggregate (e.g. COUNT, SUM, AVG, MIN, MAX) used as a window function. The result for a row is the aggregate over
 * the rows of that row's [frame] (SQL:2011 Section 10.9, General Rule 1b), computed with the same accumulators and
 * NULL/MISSING/DISTINCT handling as group aggregation (see [Aggregate.State]).
 *
 * The aggregate is computed incrementally: while consecutive frames share their first row and do not shrink (as with
 * [WindowFrame.DEFAULT]), only the newly-entered rows are fed to the running accumulator. Otherwise (e.g. a future
 * sliding frame), the frame is re-aggregated from scratch. The result is cached per frame, so all peers of a row (which
 * share a frame under [WindowFrame.DEFAULT]) are only computed once.
 *
 * This relies on [org.partiql.spi.function.Accumulator.value] being side-effect free so that it may be called between
 * calls to [org.partiql.spi.function.Accumulator.next]; this holds for the built-in COUNT/SUM/AVG/MIN/MAX accumulators,
 * which are the only aggregates that can currently be used as window functions.
 */
internal class AggregateWindowFunction(
    private val aggregate: Aggregate,
    private val frame: WindowFrame = WindowFrame.DEFAULT,
) : WindowFunction {

    private lateinit var partition: WindowPartition
    private var currentRow: Long = -1L

    /**
     * The running aggregation over the rows [accumulatedStart, accumulatedEnd] of the partition.
     */
    private var state: Aggregate.State? = null
    private var accumulatedStart: Long = 0L
    private var accumulatedEnd: Long = -1L

    private var cachedFrame: LongRange? = null
    private var cachedValue: Datum? = null

    override fun reset(partition: WindowPartition) {
        this.partition = partition
        currentRow = -1L
        state = null
        accumulatedStart = 0L
        accumulatedEnd = -1L
        cachedFrame = null
        cachedValue = null
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        currentRow++
        val bounds = frame.bounds(currentRow, orderingGroupStart, orderingGroupEnd, partition.size())
        val cached = cachedValue
        if (cached != null && bounds == cachedFrame) {
            return cached
        }
        val value = when {
            // An empty frame aggregates no rows (e.g. COUNT -> 0, SUM -> NULL), same as an empty group.
            bounds.isEmpty() -> aggregate.newState().value()
            else -> aggregate(env, bounds)
        }
        cachedFrame = bounds
        cachedValue = value
        return value
    }

    private fun aggregate(env: Environment, bounds: LongRange): Datum {
        var current = state
        if (current == null || bounds.first != accumulatedStart || bounds.last < accumulatedEnd) {
            current = aggregate.newState()
            state = current
            accumulatedStart = bounds.first
            accumulatedEnd = bounds.first - 1
        }
        for (i in (accumulatedEnd + 1)..bounds.last) {
            checkInterrupted()
            current.accumulate(env.push(partition[i]))
        }
        accumulatedEnd = bounds.last
        return current.value()
    }
}
