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
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * The LAST_VALUE window function. See SQL:2011 6.10 General Rule 1.c.
 *
 * NOTE: with the default window frame (RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW), this returns the value of
 * the last peer of the current row, NOT the value of the last row of the partition.
 */
internal class LastValueFunction(
    value: ExprValue,
    ignoreNulls: Boolean,
    returnType: PType,
) : FrameValueFunction(value, ignoreNulls, returnType) {

    override fun evalFrame(env: Environment, frameStart: Long, frameEnd: Long): Datum {
        return nthValue(frameStart, frameEnd, 1L, fromLast = true)
    }
}
