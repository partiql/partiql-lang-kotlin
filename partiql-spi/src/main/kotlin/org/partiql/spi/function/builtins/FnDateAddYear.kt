// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.errors.DataException
import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_DATE_ADD_YEAR__INT32_DATE__DATE = FunctionUtils.hidden(

    name = "date_add_year",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1].localDate
    val datetimeValue = datetime
    val intervalValue = interval.toLong()
    Datum.date(datetimeValue.plusYears(intervalValue))
}

internal val Fn_DATE_ADD_YEAR__INT64_DATE__DATE = FunctionUtils.hidden(

    name = "date_add_year",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val interval = args[0].long
    val datetime = args[1].localDate
    val datetimeValue = datetime
    val intervalValue = interval
    Datum.date(datetimeValue.plusYears(intervalValue))
}

internal val Fn_DATE_ADD_YEAR__INT_DATE__DATE = FunctionUtils.hidden(

    name = "date_add_year",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("interval", DefaultNumeric.NUMERIC),
        Parameter("datetime", PType.date()),
    ),

) { args ->
    val interval = args[0].bigDecimal
    val datetime = args[1].localDate
    val datetimeValue = datetime
    val intervalValue = try {
        interval.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.date(datetimeValue.plusYears(intervalValue))
}

internal val Fn_DATE_ADD_YEAR__INT32_TIMESTAMP__TIMESTAMP = FunctionUtils.hidden(

    name = "date_add_year",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1].localDateTime
    val datetimeValue = datetime
    val intervalValue = interval.toLong()
    Datum.timestamp(datetimeValue.plusYears(intervalValue), 6)
}

internal val Fn_DATE_ADD_YEAR__INT64_TIMESTAMP__TIMESTAMP = FunctionUtils.hidden(

    name = "date_add_year",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].long
    val datetime = args[1].localDateTime
    val datetimeValue = datetime
    val intervalValue = interval
    Datum.timestamp(datetimeValue.plusYears(intervalValue), 6)
}

internal val Fn_DATE_ADD_YEAR__INT_TIMESTAMP__TIMESTAMP = FunctionUtils.hidden(

    name = "date_add_year",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", DefaultNumeric.NUMERIC),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].bigDecimal
    val datetime = args[1].localDateTime
    val datetimeValue = datetime
    val intervalValue = try {
        interval.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.timestamp(datetimeValue.plusYears(intervalValue), 6)
}
