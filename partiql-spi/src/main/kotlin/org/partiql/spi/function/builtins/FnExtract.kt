// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum
import java.math.BigDecimal

//
// Extract Year
//
internal val Fn_EXTRACT_YEAR__DATE__INT32 = FunctionUtils.hidden(
    name = "extract_year",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.date()),
    ),
) { args ->
    val v = args[0].localDate
    Datum.integer(v.year)
}

internal val Fn_EXTRACT_YEAR__TIMESTAMP__INT32 = FunctionUtils.hidden(
    name = "extract_year",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),
) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.year)
}

internal val Fn_EXTRACT_YEAR__INTERVAL__INT32 = FunctionUtils.hidden(
    name = "extract_year",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("interval", PType.intervalYear(6)),
    ),
){ args ->
    val v = args[0]
    Datum.integer(v.years)
}

//
// Extract Month
//
internal val Fn_EXTRACT_MONTH__DATE__INT32 = FunctionUtils.hidden(
    name = "extract_month",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.date()),
    ),
) { args ->
    val v = args[0].localDate
    Datum.integer(v.monthValue)
}

internal val Fn_EXTRACT_MONTH__TIMESTAMP__INT32 = FunctionUtils.hidden(
    name = "extract_month",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),
) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.monthValue)
}

internal val Fn_EXTRACT_MONTH__INTERVAL__INT32 = FunctionUtils.hidden(
    name = "extract_month",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("interval", PType.intervalYear(6)),
    ),
){ args ->
    val v = args[0]
    Datum.integer(v.months)
}

//
//  Extract Day
//

internal val Fn_EXTRACT_DAY__DATE__INT32 = FunctionUtils.hidden(
    name = "extract_day",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.date()),
    ),
) { args ->
    val v = args[0].localDate
    Datum.integer(v.dayOfMonth)
}

internal val Fn_EXTRACT_DAY__TIMESTAMP__INT32 = FunctionUtils.hidden(
    name = "extract_day",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),
) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.dayOfMonth)
}

internal val Fn_EXTRACT_DAY__INTERVAL__INT32 = FunctionUtils.hidden(
    name = "extract_day",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("interval", PType.intervalDay(6)),
    ),
){ args ->
    val v = args[0]
    Datum.integer(v.days)
}

//
// Extract Hour
//
internal val Fn_EXTRACT_HOUR__TIME__INT32 = FunctionUtils.hidden(
    name = "extract_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),
) { args ->
    val v = args[0].localTime
    Datum.integer(v.hour)
}

internal val Fn_EXTRACT_HOUR__TIMESTAMP__INT32 = FunctionUtils.hidden(
    name = "extract_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),
) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.hour)
}

internal val Fn_EXTRACT_HOUR__INTERVAL__INT32 = FunctionUtils.hidden(
    name = "extract_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("interval", PType.intervalDay(6)),
    ),
){ args ->
    val v = args[0]
    Datum.integer(v.hours)
}

//
// Extract Minute
//
internal val Fn_EXTRACT_MINUTE__TIME__INT32 = FunctionUtils.hidden(
    name = "extract_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),
) { args ->
    val v = args[0].localTime
    Datum.integer(v.minute)
}

internal val Fn_EXTRACT_MINUTE__TIMESTAMP__INT32 = FunctionUtils.hidden(
    name = "extract_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),
) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.minute)
}

internal val Fn_EXTRACT_MINUTE__INTERVAL__INT32 = FunctionUtils.hidden(
    name = "extract_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("interval", PType.intervalDay(6)),
    ),
){ args ->
    val v = args[0]
    Datum.integer(v.minutes)
}

//
// Extract Second.
//
// Rules:
// - The declared type of the result is exact numeric with implementation-defined precision and scale.
// - The implementation-defined scale shall not be less than the fractional seconds precision of the source.
//
// Seconds is limited to [0-59].000_000_000 so DECIMAL(11,9) for now.
//
// We could return the exact precision/scale of the input type, but kiss/scope.
//
internal val Fn_EXTRACT_SECOND__TIME__DECIMAL_ARBITRARY = FunctionUtils.hidden(
    name = "extract_second",
    returns = PType.decimal(11, 9), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(
        Parameter("datetime", PType.time(9)),
    ),
) { args ->
    val v = args[0].localTime
    val d = BigDecimal(v.second).add(BigDecimal(v.nano).scaleByPowerOfTen(-9))
    Datum.decimal(d, 11, 9)
}

internal val Fn_EXTRACT_SECOND__TIMESTAMP__DECIMAL_ARBITRARY = FunctionUtils.hidden(
    name = "extract_second",
    returns = PType.decimal(11, 9), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),
) { args ->
    val v = args[0].localDateTime
    val d = BigDecimal(v.second).add(BigDecimal(v.nano).scaleByPowerOfTen(-9))
    Datum.decimal(d, 11, 9)
}

internal val Fn_EXTRACT_SECOND__INTERVAL__DECIMAL_ARBITRARY = FunctionUtils.hidden(
    name = "extract_second",
    returns = PType.decimal(11, 9), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(
        Parameter("interval", PType.intervalDay(6)),
    ),
) { args ->
    val v = args[0]
    val d = BigDecimal(v.seconds).add(BigDecimal(v.nanos).scaleByPowerOfTen(-9))
    Datum.decimal(d, 11, 9)
}

//
// Extract Timezone Hour
//
internal val Fn_EXTRACT_TIMEZONE_HOUR__TIME__INT32 = FunctionUtils.hidden(
    name = "extract_timezone_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timez(6)),
    ),
) { args ->
    val v = args[0].offsetTime
    val o = v.offset.totalSeconds / 3600
    Datum.integer(o)
}

internal val Fn_EXTRACT_TIMEZONE_HOUR__TIMESTAMP__INT32 = FunctionUtils.hidden(
    name = "extract_timezone_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestampz(6)),
    ),
) { args ->
    val v = args[0].offsetDateTime
    val o = v.offset.totalSeconds / 3600
    Datum.integer(o)
}

//
// Extract Timezone Minute
//
internal val Fn_EXTRACT_TIMEZONE_MINUTE__TIME__INT32 = FunctionUtils.hidden(
    name = "extract_timezone_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timez(6)),
    ),
) { args ->
    val v = args[0].offsetTime
    val o = (v.offset.totalSeconds / 60) % 60
    Datum.integer(o)
}

internal val Fn_EXTRACT_TIMEZONE_MINUTE__TIMESTAMP__INT32 = FunctionUtils.hidden(
    name = "extract_timezone_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestampz(6)),
    ),
) { args ->
    val v = args[0].offsetDateTime
    val o = (v.offset.totalSeconds / 60) % 60
    Datum.integer(o)
}
