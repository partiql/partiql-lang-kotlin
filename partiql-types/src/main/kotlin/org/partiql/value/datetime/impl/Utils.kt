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
