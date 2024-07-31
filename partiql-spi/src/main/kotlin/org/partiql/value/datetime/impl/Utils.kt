/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.value.datetime.impl

import java.math.BigDecimal
import java.math.RoundingMode

internal object Utils {
    // For low precision only, do not use for high precision implementation, as longValueExact will not work
    fun getSecondAndNanoFromDecimalSecond(decimalSecond: BigDecimal): Pair<Long, Long> {
        val wholeSecond = decimalSecond.setScale(0, RoundingMode.DOWN)
        val nano = decimalSecond.minus(wholeSecond).movePointRight(9)
        return (wholeSecond.longValueExact() to nano.longValueExact())
    }

    fun getDecimalSecondFromSecondAndNano(second: Long, nano: Long): BigDecimal =
        second.toBigDecimal() + nano.toBigDecimal().movePointLeft(9)
}
