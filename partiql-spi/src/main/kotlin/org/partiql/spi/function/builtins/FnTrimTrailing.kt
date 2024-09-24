// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.StringUtils.codepointTrimTrailing
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_TRIM_TRAILING__STRING__STRING = Function.standard(

    name = "trim_trailing",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

    ) { args ->
    val value = args[0].string
    val result = value.codepointTrimTrailing()
    Datum.string(result)
}

internal val Fn_TRIM_TRAILING__SYMBOL__SYMBOL = Function.standard(

    name = "trim_trailing",
    returns = PType.symbol(),
    parameters = arrayOf(Parameter("value", PType.symbol())),

    ) { args ->
    val value = args[0].string
    val result = value.codepointTrimTrailing()
    Datum.symbol(result)
}

internal val Fn_TRIM_TRAILING__CLOB__CLOB = Function.standard(

    name = "trim_trailing",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

    ) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val result = string.codepointTrimTrailing()
    Datum.clob(result.toByteArray())
}
