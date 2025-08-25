// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.IntervalUtils

internal val Fn_DATE_DIFF__STRING_DATE_DATE__INT64 = FunctionUtils.hidden(
    name = "date_diff",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("datetime1", PType.date()),
        Parameter("datetime2", PType.date()),
    ),

) { args ->

    IntervalUtils.dateDiffHelper(args[0].string, args[1], args[2])
}

internal val Fn_DATE_DIFF__STRING_TIMESTAMP_TIMESTAMP__INT64 = FunctionUtils.hidden(
    name = "date_diff",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("datetime1", PType.timestamp(6)),
        Parameter("datetime2", PType.timestamp(6)),
    ),
) { args ->

    IntervalUtils.dateDiffHelper(args[0].string, args[1], args[2])
}

internal val Fn_DATE_DIFF__STRING_TIME_TIME__INT64 = FunctionUtils.hidden(
    name = "date_diff",
    returns = PType.date(),
    parameters = arrayOf(
        Parameter("intervalType", PType.string()),
        Parameter("datetime1", PType.time(6)),
        Parameter("datetime2", PType.time(6)),
    ),

) { args ->

    IntervalUtils.dateDiffHelper(args[0].string, args[1], args[2])
}
