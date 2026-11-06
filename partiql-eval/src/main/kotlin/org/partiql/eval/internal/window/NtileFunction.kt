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
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigInteger

/**
 * The NTILE window function. See SQL:2011 6.10 General Rule 1.a.
 *
 * Divides the rows of each partition (ordered by the window ordering) into [tiles] buckets numbered 1 through NT,
 * where the first `MOD(CT, NT)` buckets hold `CEILING(CT / NT)` rows and the remaining buckets hold `FLOOR(CT / NT)` rows
 * (CT being the number of rows of the partition).
 *
 * The <number of tiles> is evaluated once per partition (against the partition's first row). The SQL specification only
 * permits a <simple value specification> or <dynamic parameter specification>, so for conforming queries this value is
 * the same for every row.
 *
 * @param tiles the number of tiles
 */
internal class NtileFunction(private val tiles: ExprValue) : WindowFunction {

    private lateinit var _partition: WindowPartition

    /**
     * The 0-based row number of the current row within the partition.
     */
    private var _index: Long = -1

    /**
     * The number of tiles for the current partition; null when the number of tiles is the null value; only valid when
     * [_tilesEvaluated] is true.
     */
    private var _tiles: Long? = null
    private var _tilesEvaluated: Boolean = false

    override fun reset(partition: WindowPartition) {
        _partition = partition
        _index = -1
        _tiles = null
        _tilesEvaluated = false
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        _index++
        if (!_tilesEvaluated) {
            _tiles = evalTiles(env)
            _tilesEvaluated = true
        }
        val nt = _tiles ?: return Datum.nullValue(PType.bigint())
        return Datum.bigint(bucket(_index, _partition.size(), nt))
    }

    /**
     * @return the number of tiles; null if the number of tiles is the null value.
     * @throws org.partiql.spi.errors.PRuntimeException if the number of tiles is less than or equal to 0.
     */
    private fun evalTiles(env: Environment): Long? {
        val datum = tiles.eval(env)
        if (datum.isNull || datum.isMissing) {
            return null
        }
        val value = datum.getBigIntCoerced()
        if (value.signum() <= 0) {
            throw PErrors.internalErrorException(
                IllegalArgumentException("Invalid argument for NTILE function: the number of tiles must be greater than 0, but was $value.")
            )
        }
        // Any number of tiles greater than the partition size behaves identically; clamp to avoid overflow.
        return if (value > LONG_MAX) Long.MAX_VALUE else value.toLong()
    }

    private companion object {
        private val LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE)

        /**
         * @param index the 0-based row number of the row within the partition
         * @param size the number of rows within the partition (CT)
         * @param tiles the number of tiles (NT); must be greater than 0
         * @return the 1-based bucket number of the row at [index]
         */
        private fun bucket(index: Long, size: Long, tiles: Long): Long {
            if (tiles >= size) {
                return index + 1
            }
            val small = size / tiles // FLOOR(CT / NT)
            val numLarge = size % tiles // MOD(CT, NT), the number of buckets with CEILING(CT / NT) = small + 1 rows
            val largeRows = numLarge * (small + 1)
            return when {
                index < largeRows -> index / (small + 1) + 1
                else -> numLarge + (index - largeRows) / small + 1
            }
        }
    }
}
