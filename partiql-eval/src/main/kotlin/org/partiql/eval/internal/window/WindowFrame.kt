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

/**
 * Computes the window frame (SQL:2011 Section 7.11, <window frame clause>) of a row within its partition.
 *
 * Window frame clauses are not yet supported (https://github.com/partiql/partiql-lang-kotlin/issues/1837), so only
 * [DEFAULT] exists today; explicit ROWS/RANGE/GROUPS frames can be added as further implementations without changing
 * the [org.partiql.eval.WindowFunction] interface.
 */
internal fun interface WindowFrame {

    /**
     * @param row the 0-based index of the current row within the partition.
     * @param peerGroupStart the index of the first row of the current row's peer (ordering) group.
     * @param peerGroupEnd the index of the last row of the current row's peer (ordering) group.
     * @param partitionSize the number of rows in the partition.
     * @return the inclusive range of row indices in the current row's frame; empty if the frame has no rows.
     */
    fun bounds(row: Long, peerGroupStart: Long, peerGroupEnd: Long, partitionSize: Long): LongRange

    companion object {

        /**
         * The frame used when no window frame clause is specified (SQL:2011 Section 7.11, Syntax Rule 10):
         * `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`, i.e. from the first row of the partition through the
         * last peer of the current row. Without a window ORDER BY all rows of the partition are peers, so this is the
         * whole partition.
         */
        val DEFAULT: WindowFrame = WindowFrame { _, _, peerGroupEnd, _ -> 0L..peerGroupEnd }
    }
}
