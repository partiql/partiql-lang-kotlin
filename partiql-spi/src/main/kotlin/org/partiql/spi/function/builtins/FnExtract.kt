// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

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
    val v = args[0].localDate
    Datum.integer(v.year)
}

internal val Fn_EXTRACT_YEAR__TIMESTAMP__INT32 = Function.static(

    name = "extract_year",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].localDateTime
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
    val v = args[0].localDate
    Datum.integer(v.monthValue)
}

internal val Fn_EXTRACT_MONTH__TIMESTAMP__INT32 = Function.static(

    name = "extract_month",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.monthValue)
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
    val v = args[0].localDate
    Datum.integer(v.dayOfMonth)
}

internal val Fn_EXTRACT_DAY__TIMESTAMP__INT32 = Function.static(

    name = "extract_day",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.dayOfMonth)
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
    val v = args[0].localTime
    Datum.integer(v.hour)
}

internal val Fn_EXTRACT_HOUR__TIMESTAMP__INT32 = Function.static(

    name = "extract_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].localDateTime
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
    val v = args[0].localTime
    Datum.integer(v.minute)
}

internal val Fn_EXTRACT_MINUTE__TIMESTAMP__INT32 = Function.static(

    name = "extract_minute",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].localDateTime
    Datum.integer(v.minute)
}

//
// Extract Second
//
internal val Fn_EXTRACT_SECOND__TIME__DECIMAL_ARBITRARY = Function.static(

    name = "extract_second",
    returns = PType.decimal(38, 19), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val v = args[0].localTime
    Datum.decimal(v.second.toBigDecimal())
}

internal val Fn_EXTRACT_SECOND__TIMESTAMP__DECIMAL_ARBITRARY = Function.static(

    name = "extract_second",
    returns = PType.decimal(38, 19), // TODO: Rewrite using new function modeling.
    parameters = arrayOf(
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val v = args[0].localTime
    // TODO this doesn't handle nanoseconds
    Datum.decimal(v.second.toBigDecimal())
}

//
// Extract Timezone Hour
//
internal val Fn_EXTRACT_TIMEZONE_HOUR__TIME__INT32 = Function.static(

    name = "extract_timezone_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timez(6)),
    ),

) { args ->
    val v = args[0].offsetTime
    val hours = v.offset.totalSeconds.floorDiv(3600)
    Datum.integer(hours)
}

internal val Fn_EXTRACT_TIMEZONE_HOUR__TIMESTAMP__INT32 = Function.static(

    name = "extract_timezone_hour",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timestampz(6)),
    ),

) { args ->
    val v = args[0].offsetTime
    val hours = v.offset.totalSeconds.floorDiv(3600)
    Datum.integer(hours)
}

//
// Extract Timezone Minute
//
internal val Fn_EXTRACT_TIMEZONE_MINUTE__TIME__INT32 = Function.static(
    name = "extract_timezone_minute", returns = PType.integer(),
    parameters = arrayOf(
        Parameter("datetime", PType.timez(6)),
    )
) { args ->
    val v = args[0].offsetTime
    val m = v.offset.totalSeconds.floorDiv(60)
    Datum.integer(m)
}

internal val Fn_EXTRACT_TIMEZONE_MINUTE__TIMESTAMP__INT32 = Function.static(
    name = "extract_timezone_minute",
    returns = PType.integer(),
    parameters = arrayOf(Parameter("datetime", PType.timestampz(6))),
) { args ->
    val v = args[0].offsetTime
    val m = v.offset.totalSeconds.floorDiv(60)
    Datum.integer(m)
}
