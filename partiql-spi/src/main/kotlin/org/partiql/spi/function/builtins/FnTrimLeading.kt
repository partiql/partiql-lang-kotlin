// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.StringUtils.codepointTrimLeading
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_TRIM_LEADING__STRING__STRING = Function.standard(

    name = "trim_leading",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

    ) { args ->
    val value = args[0].string
    val result = value.codepointTrimLeading()
    Datum.string(result)
}

internal val Fn_TRIM_LEADING__SYMBOL__SYMBOL = Function.standard(

    name = "trim_leading",
    returns = PType.symbol(),
    parameters = arrayOf(Parameter("value", PType.symbol())),

    ) { args ->
    val value = args[0].string
    val result = value.codepointTrimLeading()
    Datum.symbol(result)
}

internal val Fn_TRIM_LEADING__CLOB__CLOB = Function.standard(

    name = "trim_leading",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

    ) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val result = string.codepointTrimLeading()
    Datum.clob(result.toByteArray())
}
