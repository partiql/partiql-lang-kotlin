// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_DATE_ADD_MONTH__INT32_DATE__DATE = Function.static(

    name = "date_add_month",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1].date
    val datetimeValue = datetime
    val intervalValue = interval.toLong()
    Datum.date(datetimeValue.plusMonths(intervalValue))
}

internal val Fn_DATE_ADD_MONTH__INT64_DATE__DATE = Function.static(

    name = "date_add_month",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val interval = args[0].long
    val datetime = args[1].date
    val datetimeValue = datetime
    val intervalValue = interval
    Datum.date(datetimeValue.plusMonths(intervalValue))
}

internal val Fn_DATE_ADD_MONTH__INT_DATE__DATE = Function.static(

    name = "date_add_month",
    returns = PType.date(),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("interval", PType.numeric()),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val interval = args[0].bigInteger
    val datetime = args[1].date
    val datetimeValue = datetime
    val intervalValue = try {
        interval.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.date(datetimeValue.plusMonths(intervalValue))
}

internal val Fn_DATE_ADD_MONTH__INT32_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_month",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1].timestamp
    val datetimeValue = datetime
    val intervalValue = interval.toLong()
    Datum.timestamp(datetimeValue.plusMonths(intervalValue))
}

internal val Fn_DATE_ADD_MONTH__INT64_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_month",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].long
    val datetime = args[1].timestamp
    val datetimeValue = datetime
    val intervalValue = interval
    Datum.timestamp(datetimeValue.plusMonths(intervalValue))
}

internal val Fn_DATE_ADD_MONTH__INT_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_month",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("interval", PType.numeric()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].bigInteger
    val datetime = args[1].timestamp
    val datetimeValue = datetime
    val intervalValue = try {
        interval.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.timestamp(datetimeValue.plusMonths(intervalValue))
}
