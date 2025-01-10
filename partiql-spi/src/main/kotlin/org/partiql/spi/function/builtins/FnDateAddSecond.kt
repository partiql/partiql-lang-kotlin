// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.errors.DataException
import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_DATE_ADD_SECOND__INT32_TIME__TIME = FunctionUtils.hidden(

    name = "date_add_second",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1].localTime
    val datetimeValue = datetime
    val intervalValue = interval.toLong()
    Datum.time(datetimeValue.plusSeconds(intervalValue), 6)
}

internal val Fn_DATE_ADD_SECOND__INT64_TIME__TIME = FunctionUtils.hidden(

    name = "date_add_second",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0].long
    val datetime = args[1].localTime
    val datetimeValue = datetime
    val intervalValue = interval
    Datum.time(datetimeValue.plusSeconds(intervalValue), 6)
}

internal val Fn_DATE_ADD_SECOND__INT_TIME__TIME = FunctionUtils.hidden(

    name = "date_add_second",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", DefaultNumeric.NUMERIC),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0].bigDecimal
    val datetime = args[1].localTime
    val datetimeValue = datetime
    val intervalValue = try {
        interval.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.time(datetimeValue.plusSeconds(intervalValue), 6)
}

internal val Fn_DATE_ADD_SECOND__INT32_TIMESTAMP__TIMESTAMP = FunctionUtils.hidden(

    name = "date_add_second",
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
    Datum.timestamp(datetimeValue.plusSeconds(intervalValue), 6)
}

internal val Fn_DATE_ADD_SECOND__INT64_TIMESTAMP__TIMESTAMP = FunctionUtils.hidden(

    name = "date_add_second",
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
    Datum.timestamp(datetimeValue.plusSeconds(intervalValue), 6)
}

internal val Fn_DATE_ADD_SECOND__INT_TIMESTAMP__TIMESTAMP = FunctionUtils.hidden(

    name = "date_add_second",
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
    Datum.timestamp(datetimeValue.plusSeconds(intervalValue), 6)
}
