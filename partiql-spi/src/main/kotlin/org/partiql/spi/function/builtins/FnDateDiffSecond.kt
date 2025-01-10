// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType

internal val Fn_DATE_DIFF_SECOND__TIME_TIME__INT64 = FunctionUtils.hidden(

    name = "date_diff_second",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.time(6)),
        Parameter("datetime2", PType.time(6)),
    ),

) { args ->
    TODO("Function date_diff_second not implemented")
}

internal val Fn_DATE_DIFF_SECOND__TIMESTAMP_TIMESTAMP__INT64 = FunctionUtils.hidden(

    name = "date_diff_second",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timestamp(6)),
        Parameter("datetime2", PType.timestamp(6)),
    ),

) { args ->
    TODO("Function date_diff_second not implemented")
}
