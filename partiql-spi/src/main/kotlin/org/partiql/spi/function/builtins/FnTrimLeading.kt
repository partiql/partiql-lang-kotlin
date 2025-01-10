// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.function.utils.StringUtils.codepointTrimLeading
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_TRIM_LEADING__STRING__STRING = FunctionUtils.hidden(

    name = "trim_leading",
    returns = PType.string(),
    parameters = arrayOf(Parameter("value", PType.string())),

) { args ->
    val value = args[0].string
    val result = value.codepointTrimLeading()
    Datum.string(result)
}

internal val Fn_TRIM_LEADING__CLOB__CLOB = FunctionUtils.hidden(

    name = "trim_leading",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(Parameter("value", PType.clob(Int.MAX_VALUE))),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val result = string.codepointTrimLeading()
    Datum.clob(result.toByteArray())
}
