// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal val Fn_DATE_DIFF_MONTH__DATE_DATE__INT64 = Function.standard(

    name = "date_diff_month",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.date()),
        Parameter("datetime2", PType.date()),
    ),

    ) { args ->
    TODO("Function date_diff_month not implemented")
}

internal val Fn_DATE_DIFF_MONTH__TIMESTAMP_TIMESTAMP__INT64 = Function.standard(

    name = "date_diff_month",
    returns = PType.bigint(),
    parameters = arrayOf(
        Parameter("datetime1", PType.timestamp(6)),
        Parameter("datetime2", PType.timestamp(6)),
    ),

    ) { args ->
    TODO("Function date_diff_month not implemented")
}
