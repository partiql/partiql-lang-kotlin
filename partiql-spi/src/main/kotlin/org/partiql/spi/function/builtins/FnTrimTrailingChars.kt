// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.StringUtils.codepointTrimTrailing
import org.partiql.spi.value.Datum

internal val Fn_TRIM_TRAILING_CHARS__STRING_STRING__STRING = FunctionUtils.hidden(

    name = "trim_trailing_chars",
    returns = PType.string(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
        Parameter("chars", PType.string()),
    ),

) { args ->
    val value = args[0].string
    val chars = args[1].string
    val result = value.codepointTrimTrailing(chars)
    Datum.string(result)
}

internal val Fn_TRIM_TRAILING_CHARS__CLOB_CLOB__CLOB = FunctionUtils.hidden(

    name = "trim_trailing_chars",
    returns = PType.clob(Long.MAX_VALUE),
    parameters = arrayOf(
        Parameter("value", PType.clob(Long.MAX_VALUE)),
        Parameter("chars", PType.clob(Long.MAX_VALUE)),
    ),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val chars = args[1].bytes.toString(Charsets.UTF_8)
    val result = string.codepointTrimTrailing(chars)
    Datum.clob(result.toByteArray())
}
