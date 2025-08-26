// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.IntervalUtils

internal val Fn_DATE_DIFF_MINUTE__TIME_TIME__INT64 = FunctionUtils.hidden(

    name = "date_diff_minute",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.time(6)),
        Parameter("datetime2", PType.time(6)),
    ),

) { args ->
    IntervalUtils.dateDiffHelper("minute", args[0], args[1])
}

internal val Fn_DATE_DIFF_MINUTE__TIMESTAMP_TIMESTAMP__INT64 = FunctionUtils.hidden(

    name = "date_diff_minute",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timestamp(6)),
        Parameter("datetime2", PType.timestamp(6)),
    ),

) { args ->
    IntervalUtils.dateDiffHelper("minute", args[0], args[1])
}
