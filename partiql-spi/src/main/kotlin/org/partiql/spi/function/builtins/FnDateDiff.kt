// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.IntervalUtils
import org.partiql.spi.value.Datum
import org.partiql.spi.utils.IntervalUtils.INTERVAL_MAX_PRECISION

internal val Fn_DATE_DIFF__STRING_INT32_DATE__DATE = FunctionUtils.hidden(
    name = "date_add",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.date()),
    ),

    ) { args ->

    IntervalUtils.dateDiffHelper(args[0].string, args[1].int, args[2])
}

internal val Fn_DATE_DIFF__STRING_INT64_DATE__DATE = FunctionUtils.hidden(
    name = "date_add",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.date()),
    ),

    ) { args ->

    val intervalValueInLong = args[1].long
    require(intervalValueInLong in Int.MIN_VALUE..Int.MAX_VALUE) { "Value $intervalValueInLong overflows Int" }
    val intervalValue = intervalValueInLong.toInt()

    IntervalUtils.dateDiffHelper(args[0].string, intervalValue, args[2])
}

internal val Fn_DATE_DIFF__STRING_INT32_TIMESTAMP__TIMESTAMP = FunctionUtils.hidden(
    name = "date_add",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.timestamp(6)),
    ),

    ) { args ->

    IntervalUtils.dateDiffHelper(args[0].string, args[1].int, args[2])
}

internal val Fn_DATE_DIFF__STRING_INT64_TIMESTAMP__TIMESTAMP= FunctionUtils.hidden(
    name = "date_add",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.timestamp(6)),
    ),

    ) { args ->

    val intervalValueInLong = args[1].long
    require(intervalValueInLong in Int.MIN_VALUE..Int.MAX_VALUE) { "Value $intervalValueInLong overflows Int" }
    val intervalValue = intervalValueInLong.toInt()

    IntervalUtils.dateDiffHelper(args[0].string, intervalValue, args[2])
}

internal val Fn_DATE_DIFF__STRING_INT32_TIME__TIME = FunctionUtils.hidden(
    name = "date_add",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("interval", PType.integer()),
        Parameter("datetime", PType.time(6)),
    ),

    ) { args ->

    IntervalUtils.dateDiffHelper(args[0].string, args[1].int, args[2])
}

internal val Fn_DATE_DIFF__STRING_INT64_TIME__TIME= FunctionUtils.hidden(
    name = "date_add",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("interval", PType.bigint()),
        Parameter("datetime", PType.time(6)),
    ),

    ) { args ->

    val intervalValueInLong = args[1].long
    require(intervalValueInLong in Int.MIN_VALUE..Int.MAX_VALUE) { "Value $intervalValueInLong overflows Int" }
    val intervalValue = intervalValueInLong.toInt()

    IntervalUtils.dateDiffHelper(args[0].string, intervalValue, args[2])
}

