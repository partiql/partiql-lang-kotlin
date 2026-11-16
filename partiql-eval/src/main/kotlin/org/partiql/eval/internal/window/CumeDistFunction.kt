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
import org.partiql.spi.value.Datum

/**
 * CUME_DIST() as defined by SQL:2023 (ISO/IEC 9075-2) Subclause 9.23, Syntax Rule 5h:
 * np / n, where np is the number of rows preceding or peer with the current row, and n is the number of rows in
 * the partition.
 *
 * The number of rows preceding or peer with the current row is the (zero-based) index of the last row of the
 * current row's peer group plus one.
 */
internal class CumeDistFunction : PartitionSizedFunction() {

    override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum {
        return Datum.doublePrecision((orderingGroupEnd + 1).toDouble() / partitionSize.toDouble())
    }
}
