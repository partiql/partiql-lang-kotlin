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
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.getBigIntCoerced
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigInteger

/**
 * The NTH_VALUE window function. See SQL:2011 6.10 General Rule 1.d.
 *
 * The <nth row> is evaluated for each row (against the current row). The SQL specification only permits a
 * <simple value specification> or <dynamic parameter specification>, so for conforming queries this value is the same
 * for every row.
 *
 * @param nthRow the (1-based) position within the window frame
 * @param fromLast an expression evaluating to a boolean; true iff FROM LAST is specified
 */
internal class NthValueFunction(
    value: ExprValue,
    private val nthRow: ExprValue,
    private val fromLast: ExprValue,
    ignoreNulls: Boolean,
    returnType: PType,
) : FrameValueFunction(value, ignoreNulls, returnType) {

    override fun evalFrame(env: Environment, frameStart: Long, frameEnd: Long): Datum {
        val rn = nthRow.eval(env)
        if (rn.isNull || rn.isMissing) {
            return nullResult
        }
        val n = rn.getBigIntCoerced()
        if (n.signum() <= 0) {
            throw PErrors.internalErrorException(
                IllegalArgumentException("Invalid argument for NTH_VALUE function: the nth row must be greater than 0, but was $n.")
            )
        }
        // Any position larger than the window frame yields the null value; clamp to avoid overflow.
        val nLong = if (n > LONG_MAX) Long.MAX_VALUE else n.toLong()
        val isFromLast = fromLast.eval(env).boolean
        return nthValue(frameStart, frameEnd, nLong, isFromLast)
    }

    private companion object {
        private val LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE)
    }
}
