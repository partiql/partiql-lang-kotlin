// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_DATE_ADD_DAY__INT32_DATE__DATE = Function.static(

    name = "date_add_day",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val days = args[0].int
    val date = args[1].localDate
    Datum.date(date.plusDays(days.toLong()))
}

internal val Fn_DATE_ADD_DAY__INT64_DATE__DATE = Function.static(

    name = "date_add_day",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val days = args[0].long
    val date = args[1].localDate
    Datum.date(date.plusDays(days))
}

internal val Fn_DATE_ADD_DAY__INT32_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_day",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val days = args[0].int
    val timestamp = args[1].localDateTime
    Datum.timestamp(timestamp.plusDays(days.toLong()), 6)
}

internal val Fn_DATE_ADD_DAY__INT64_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_day",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val days = args[0].long
    val timestamp = args[1].localDateTime
    Datum.timestamp(timestamp.plusDays(days), 6)
}
