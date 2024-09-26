// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import org.partiql.value.datetime.TimeZone

//
// Extract Year
//
internal val Fn_EXTRACT_YEAR__DATE__INT32 = Function.static(

    name = "extract_year",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val v = args[0].date
    Datum.integer(v.year)
}

internal val Fn_EXTRACT_YEAR__TIMESTAMP__INT32 = Function.static(

    name = "extract_year",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    Datum.integer(v.year)
}

//
// Extract Month
//
internal val Fn_EXTRACT_MONTH__DATE__INT32 = Function.static(

    name = "extract_month",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val v = args[0].date
    Datum.integer(v.month)
}

internal val Fn_EXTRACT_MONTH__TIMESTAMP__INT32 = Function.static(

    name = "extract_month",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    Datum.integer(v.month)
}

//
//  Extract Day
//

internal val Fn_EXTRACT_DAY__DATE__INT32 = Function.static(

    name = "extract_day",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val v = args[0].date
    Datum.integer(v.day)
}

internal val Fn_EXTRACT_DAY__TIMESTAMP__INT32 = Function.static(

    name = "extract_day",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    Datum.integer(v.day)
}

//
// Extract Hour
//
internal val Fn_EXTRACT_HOUR__TIME__INT32 = Function.static(

    name = "extract_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val v = args[0].time
    Datum.integer(v.hour)
}

internal val Fn_EXTRACT_HOUR__TIMESTAMP__INT32 = Function.static(

    name = "extract_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    Datum.integer(v.hour)
}

//
// Extract Minute
//
internal val Fn_EXTRACT_MINUTE__TIME__INT32 = Function.static(

    name = "extract_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val v = args[0].time
    Datum.integer(v.minute)
}

internal val Fn_EXTRACT_MINUTE__TIMESTAMP__INT32 = Function.static(

    name = "extract_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    Datum.integer(v.minute)
}

//
// Extract Second
//
internal val Fn_EXTRACT_SECOND__TIME__DECIMAL_ARBITRARY = Function.static(

    name = "extract_second",
    returns = PType.decimal(),
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val v = args[0].time
    Datum.decimal(v.decimalSecond)
}

internal val Fn_EXTRACT_SECOND__TIMESTAMP__DECIMAL_ARBITRARY = Function.static(

    name = "extract_second",
    returns = PType.decimal(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    Datum.decimal(v.decimalSecond)
}

//
// Extract Timezone Hour
//
internal val Fn_EXTRACT_TIMEZONE_HOUR__TIME__INT32 = Function.static(

    name = "extract_timezone_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val v = args[0].time
    when (val tz = v.timeZone) {
        TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
        is TimeZone.UtcOffset -> Datum.integer(tz.tzHour)
        null -> Datum.nullValue(PType.integer())
    }
}

internal val Fn_EXTRACT_TIMEZONE_HOUR__TIMESTAMP__INT32 = Function.static(

    name = "extract_timezone_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    when (val tz = v.timeZone) {
        TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
        is TimeZone.UtcOffset -> Datum.integer(tz.tzHour)
        null -> Datum.nullValue(PType.integer())
    }
}

//
// Extract Timezone Minute
//
internal val Fn_EXTRACT_TIMEZONE_MINUTE__TIME__INT32 = Function.static(

    name = "extract_timezone_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val v = args[0].time
    when (val tz = v.timeZone) {
        TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
        is TimeZone.UtcOffset -> Datum.integer(tz.tzMinute)
        null -> Datum.nullValue(PType.integer())
    }
}

internal val Fn_EXTRACT_TIMEZONE_MINUTE__TIMESTAMP__INT32 = Function.static(

    name = "extract_timezone_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].timestamp
    when (val tz = v.timeZone) {
        TimeZone.UnknownTimeZone -> Datum.integer(0) // TODO: Should this be NULL?
        is TimeZone.UtcOffset -> Datum.integer(tz.tzMinute)
        null -> Datum.nullValue(PType.integer())
    }
}
