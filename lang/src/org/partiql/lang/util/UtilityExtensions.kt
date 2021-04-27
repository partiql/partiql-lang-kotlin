package org.partiql.lang.util

import org.partiql.lang.eval.builtins.Time.Companion.MAX_PRECISION_FOR_TIME
import org.partiql.lang.eval.builtins.Time.Companion.SECONDS_PER_HOUR
import org.partiql.lang.eval.builtins.Time.Companion.SECONDS_PER_MINUTE
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue

/**
 * Returns the string representation of the [ZoneOffset] in HH:mm format.
 */
fun ZoneOffset.getOffsetHHmm(): String =
    (if(totalSeconds >= 0) "+" else "-") +
    (totalSeconds / SECONDS_PER_HOUR).absoluteValue.toString().padStart(2, '0') +
    ":" +
    ((totalSeconds / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE).absoluteValue.toString().padStart(2, '0')

/**
 * Gets the number of most significant digits (excluding trailing zeros) in the fractional part of the second represented by [nano] of [LocalTime]
 * For example, localtime with nano = 123000000 returns precision as 3
 */
fun LocalTime.getPrecision(): Int = when (nano) {
    0 -> 0
    else -> {
        var trailingZeros = 0
        var nanos = nano
        while (nanos % 10 == 0) {
            trailingZeros ++
            nanos /= 10
        }
        MAX_PRECISION_FOR_TIME - trailingZeros
    }
}