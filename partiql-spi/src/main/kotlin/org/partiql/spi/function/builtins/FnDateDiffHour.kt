// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum
import java.time.temporal.ChronoUnit

internal val Fn_DATE_DIFF_HOUR__TIME_TIME__INT64 = FunctionUtils.hidden(

    name = "date_diff_hour",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.time(6)),
        Parameter("datetime2", PType.time(6)),
    ),

) { args ->
    val time1 = args[0].localTime
    val time2 = args[1].localTime
    val hourDiff = ChronoUnit.HOURS.between(time1, time2)
    Datum.bigint(hourDiff)
}

internal val Fn_DATE_DIFF_HOUR__TIMESTAMP_TIMESTAMP__INT64 = FunctionUtils.hidden(

    name = "date_diff_hour",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timestamp(6)),
        Parameter("datetime2", PType.timestamp(6)),
    ),

) { args ->
    val timestamp1 = args[0].localDateTime
    val timestamp2 = args[1].localDateTime
    val hourDiff = ChronoUnit.HOURS.between(timestamp1, timestamp2)
    Datum.bigint(hourDiff)
}

internal val Fn_DATE_DIFF_HOUR__TIMESTAMPZ_TIMESTAMPZ__INT64 = FunctionUtils.hidden(

    name = "date_diff_hour",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timestampz()),
        Parameter("datetime2", PType.timestampz()),
    ),

) { args ->
    val timestampz1 = args[0].offsetDateTime
    val timestampz2 = args[1].offsetDateTime
    val hourDiff = ChronoUnit.HOURS.between(timestampz1, timestampz2)
    Datum.bigint(hourDiff)
}

internal val Fn_DATE_DIFF_HOUR__TIMEZ_TIMEZ__INT64 = FunctionUtils.hidden(

    name = "date_diff_hour",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timez(6)),
        Parameter("datetime2", PType.timez(6)),
    ),

) { args ->
    val timez1 = args[0].offsetTime
    val timez2 = args[1].offsetTime
    val hourDiff = ChronoUnit.HOURS.between(timez1, timez2)
    Datum.bigint(hourDiff)
}
