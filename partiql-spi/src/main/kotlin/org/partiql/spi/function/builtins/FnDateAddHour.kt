// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_DATE_ADD_HOUR__INT32_TIME__TIME = Function.static(

    name = "date_add_hour",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1]
    val datetimeValue = datetime.time
    val intervalValue = interval.toLong()
    Datum.time(datetimeValue.plusHours(intervalValue))
}

internal val Fn_DATE_ADD_HOUR__INT64_TIME__TIME = Function.static(

    name = "date_add_hour",
    returns = PType.time(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0]
    val datetime = args[1]
    val datetimeValue = datetime.time
    val intervalValue = interval.long
    Datum.time(datetimeValue.plusHours(intervalValue))
}

internal val Fn_DATE_ADD_HOUR__INT_TIME__TIME = Function.static(

    name = "date_add_hour",
    returns = PType.time(6),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("interval", PType.numeric()),
        Parameter("datetime", PType.time(6)),
    ),

) { args ->
    val interval = args[0]
    val datetime = args[1]
    val datetimeValue = datetime.time
    val intervalValue = try {
        interval.bigInteger.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.time(datetimeValue.plusHours(intervalValue))
}

internal val Fn_DATE_ADD_HOUR__INT32_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_hour",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0].int
    val datetime = args[1]
    val datetimeValue = datetime.timestamp
    val intervalValue = interval.toLong()
    Datum.timestamp(datetimeValue.plusHours(intervalValue))
}

internal val Fn_DATE_ADD_HOUR__INT64_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_hour",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0]
    val datetime = args[1]
    val datetimeValue = datetime.timestamp
    val intervalValue = interval.long
    Datum.timestamp(datetimeValue.plusHours(intervalValue))
}

internal val Fn_DATE_ADD_HOUR__INT_TIMESTAMP__TIMESTAMP = Function.static(

    name = "date_add_hour",
    returns = PType.timestamp(6),
    parameters = arrayOf(
        @Suppress("DEPRECATION") Parameter("interval", PType.numeric()),
        Parameter("datetime", PType.timestamp(6)),
    ),

) { args ->
    val interval = args[0]
    val datetime = args[1]
    val datetimeValue = datetime.timestamp
    val intervalValue = try {
        interval.bigInteger.toLong()
    } catch (e: DataException) {
        throw TypeCheckException()
    }
    Datum.timestamp(datetimeValue.plusHours(intervalValue))
}
