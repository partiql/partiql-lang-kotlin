// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

// TODO: add support for CHAR/VARCHAR - https://github.com/partiql/partiql-lang-kotlin/issues/1838
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.StringUtils.codepointTrim
import org.partiql.spi.value.Datum

internal val Fn_TRIM_CHARS__STRING_STRING__STRING = FunctionUtils.hidden(

    name = "trim_chars",
    returns = PType.string(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
        Parameter("chars", PType.string()),
    ),

) { args ->
    val value = args[0].string
    val chars = args[1].string
    val result = value.codepointTrim(chars)
    Datum.string(result)
}

internal val Fn_TRIM_CHARS__CLOB_CLOB__CLOB = FunctionUtils.hidden(

    name = "trim_chars",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(
        Parameter("value", PType.clob(Int.MAX_VALUE)),
        Parameter("chars", PType.clob(Int.MAX_VALUE)),
    ),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val chars = args[1].bytes.toString(Charsets.UTF_8)
    val result = string.codepointTrim(chars)
    Datum.clob(result.toByteArray())
}
