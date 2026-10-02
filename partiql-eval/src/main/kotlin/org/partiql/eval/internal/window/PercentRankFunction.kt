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
import org.partiql.spi.value.Datum

/**
 * PERCENT_RANK() as defined by SQL:2011 (ISO/IEC 9075-2) Section 6.10:
 * (rank - 1) / (n - 1), where rank is the RANK() of the current row and n is the number of rows in the partition.
 * If the partition contains a single row, the result is 0.
 *
 * Since RANK() is equal to the number of rows preceding the current row's peer group plus one, rank - 1 is
 * the (zero-based) index of the first row of the current row's peer group.
 */
internal class PercentRankFunction : WindowFunction {

    private var _partitionSize: Long = 0

    override fun reset(partition: WindowPartition) {
        _partitionSize = partition.size()
    }

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        if (_partitionSize <= 1) {
            return Datum.doublePrecision(0.0)
        }
        return Datum.doublePrecision(orderingGroupStart.toDouble() / (_partitionSize - 1).toDouble())
    }
}
