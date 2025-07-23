// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.math.absoluteValue

/*
ABS overflow behavior is specified in SQL1999 section 6.17:
    9) If <absolute value expression> is specified, then let N be the value of the immediately contained
    <numeric value expression>.
    Case:
    a) If N is the null value, then the result is the null value.
    b) If N >= 0, then the result is N.
    c) Otherwise, the result is -1 * N. If -1 * N is not representable by the result data type, then
    an exception condition is raised: data exception — numeric value out of range
 */

internal val Fn_ABS__INT8__INT8 = Function.overload(
    name = "abs",
    parameters = arrayOf(Parameter("value", PType.tinyint())),
    returns = PType.tinyint(),
) { args ->
    val value = args[0].byte
    if (value == Byte.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("ABS($value)", PType.tinyint())
    } else {
        val result = if (value < 0) (-value).toByte() else value
        Datum.tinyint(result)
    }
}

internal val Fn_ABS__INT16__INT16 = Function.overload(
    name = "abs",
    returns = PType.smallint(),
    parameters = arrayOf(Parameter("value", PType.smallint())),
) { args ->
    val value = args[0].short
    if (value == Short.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("ABS($value)", PType.smallint())
    } else {
        val result = if (value < 0) (-value).toShort() else value
        Datum.smallint(result)
    }
}

internal val Fn_ABS__INT32__INT32 = Function.overload(
    name = "abs",
    returns = PType.integer(),
    parameters = arrayOf(Parameter("value", PType.integer())),
) { args ->
    val value = args[0].int
    if (value == Int.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("ABS($value)", PType.integer())
    } else {
        Datum.integer(value.absoluteValue)
    }
}

internal val Fn_ABS__INT64__INT64 = Function.overload(
    name = "abs",
    returns = PType.bigint(),
    parameters = arrayOf(Parameter("value", PType.bigint())),
) { args ->
    val value = args[0].long
    if (value == Long.MIN_VALUE) {
        throw PErrors.numericValueOutOfRangeException("ABS($value)", PType.bigint())
    } else {
        Datum.bigint(value.absoluteValue)
    }
}

internal val Fn_ABS__NUMERIC__NUMERIC = Function.overload(
    name = "abs",
    returns = DefaultNumeric.NUMERIC,
    parameters = arrayOf(Parameter("value", DefaultNumeric.NUMERIC)),
) { args ->
    val value = args[0].bigDecimal
    Datum.numeric(value.abs())
}

internal val Fn_ABS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY = Function.overload(
    name = "abs",
    returns = PType.decimal(39, 19), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(Parameter("value", PType.decimal(38, 19))),
) { args ->
    val value = args[0].bigDecimal
    Datum.decimal(value.abs())
}

internal val Fn_ABS__FLOAT32__FLOAT32 = Function.overload(
    name = "abs",
    returns = PType.real(),
    parameters = arrayOf(Parameter("value", PType.real())),
) { args ->
    val value = args[0].float
    Datum.real(value.absoluteValue)
}

internal val Fn_ABS__FLOAT64__FLOAT64 = Function.overload(
    name = "abs",
    returns = PType.doublePrecision(),
    parameters = arrayOf(Parameter("value", PType.doublePrecision())),
) { args ->
    val value = args[0].double
    Datum.doublePrecision(value.absoluteValue)
}

internal val Fn_ABS__INTERVAL_YM__INTERVAL_YM = Function.overload(
    name = "abs",
    returns = PType.intervalYearMonth(2), // Default precision 2
    parameters = arrayOf(Parameter("value", PType.intervalYearMonth(2))),
) { args ->
    val interval = args[0]
    when (interval.type.getIntervalCode()) {
        IntervalCode.YEAR -> Datum.intervalYear(
            interval.years.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.MONTH -> Datum.intervalMonth(
            interval.months.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.YEAR_MONTH -> Datum.intervalYearMonth(
            interval.years.absoluteValue,
            interval.months.absoluteValue,
            interval.type.precision,
        )
        else -> throw IllegalArgumentException("Unexpected interval code for year-month interval: ${interval.type.getIntervalCode()}")
    }
}

internal val Fn_ABS__INTERVAL_DT__INTERVAL_DT = Function.overload(
    name = "abs",
    returns = PType.intervalDaySecond(2, 6), // Default precision 2, fractional precision 6
    parameters = arrayOf(Parameter("value", PType.intervalDaySecond(2, 6))),
) { args ->
    val interval = args[0]
    val intervalCode = interval.type.getIntervalCode()
    when (intervalCode) {
        IntervalCode.DAY -> Datum.intervalDay(
            interval.days.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.HOUR -> Datum.intervalHour(
            interval.hours.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.MINUTE -> Datum.intervalMinute(
            interval.minutes.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.SECOND -> Datum.intervalSecond(
            interval.seconds.absoluteValue,
            interval.nanos.absoluteValue,
            interval.type.precision,
            interval.type.fractionalPrecision,
        )
        IntervalCode.DAY_HOUR -> Datum.intervalDayHour(
            interval.days.absoluteValue,
            interval.hours.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.DAY_MINUTE -> Datum.intervalDayMinute(
            interval.days.absoluteValue,
            interval.hours.absoluteValue,
            interval.minutes.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.DAY_SECOND -> Datum.intervalDaySecond(
            interval.days.absoluteValue,
            interval.hours.absoluteValue,
            interval.minutes.absoluteValue,
            interval.seconds.absoluteValue,
            interval.nanos.absoluteValue,
            interval.type.precision,
            interval.type.fractionalPrecision,
        )
        IntervalCode.HOUR_MINUTE -> Datum.intervalHourMinute(
            interval.hours.absoluteValue,
            interval.minutes.absoluteValue,
            interval.type.precision,
        )
        IntervalCode.HOUR_SECOND -> Datum.intervalHourSecond(
            interval.hours.absoluteValue,
            interval.minutes.absoluteValue,
            interval.seconds.absoluteValue,
            interval.nanos.absoluteValue,
            interval.type.precision,
            interval.type.fractionalPrecision,
        )
        IntervalCode.MINUTE_SECOND -> Datum.intervalMinuteSecond(
            interval.minutes.absoluteValue,
            interval.seconds.absoluteValue,
            interval.nanos.absoluteValue,
            interval.type.precision,
            interval.type.fractionalPrecision,
        )
        else -> throw IllegalArgumentException("Unexpected interval code for day-time interval: $intervalCode")
    }
}
