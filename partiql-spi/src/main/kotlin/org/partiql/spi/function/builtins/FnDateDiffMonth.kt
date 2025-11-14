// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum
import java.time.temporal.ChronoUnit

internal val Fn_DATE_DIFF_MONTH__DATE_DATE__INT64 = FunctionUtils.hidden(

    name = "date_diff_month",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.date()),
        Parameter("datetime2", PType.date()),
    ),

) { args ->
    val date1 = args[0].localDate
    val date2 = args[1].localDate
    val monthDiff = ChronoUnit.MONTHS.between(date1, date2)
    Datum.bigint(monthDiff)
}

internal val Fn_DATE_DIFF_MONTH__TIMESTAMP_TIMESTAMP__INT64 = FunctionUtils.hidden(

    name = "date_diff_month",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timestamp(6)),
        Parameter("datetime2", PType.timestamp(6)),
    ),

) { args ->
    val timestamp1 = args[0].localDateTime
    val timestamp2 = args[1].localDateTime
    val monthDiff = ChronoUnit.MONTHS.between(timestamp1, timestamp2)
    Datum.bigint(monthDiff)
}

internal val Fn_DATE_DIFF_MONTH__TIMESTAMPZ_TIMESTAMPZ__INT64 = FunctionUtils.hidden(

    name = "date_diff_month",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timestampz()),
        Parameter("datetime2", PType.timestampz()),
    ),

) { args ->
    val timestampz1 = args[0].offsetDateTime
    val timestampz2 = args[1].offsetDateTime
    val monthDiff = ChronoUnit.MONTHS.between(timestampz1, timestampz2)
    Datum.bigint(monthDiff)
}
