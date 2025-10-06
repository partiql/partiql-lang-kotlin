// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.PatternUtils.matchRegexPattern
import org.partiql.spi.utils.PatternUtils.parsePattern
import org.partiql.spi.value.Datum
import java.util.regex.Pattern

internal val Fn_LIKE__STRING_STRING__BOOL = FunctionUtils.hidden(

    name = "like",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
        Parameter("pattern", PType.string()),
    ),

) { args ->
    val value = args[0].string
    val pattern = args[1].string
    val likeRegexPattern = when {
        pattern.isEmpty() -> Pattern.compile("")
        else -> parsePattern(pattern, null)
    }
    when (matchRegexPattern(value, likeRegexPattern)) {
        true -> Datum.bool(true)
        else -> Datum.bool(false)
    }
}

internal val Fn_LIKE__CLOB_CLOB__BOOL = FunctionUtils.hidden(

    name = "like",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.clob(Long.MAX_VALUE)),
        Parameter("pattern", PType.clob(Long.MAX_VALUE)),
    ),

) { args ->
    val value = args[0].bytes.toString(Charsets.UTF_8)
    val pattern = args[1].bytes.toString(Charsets.UTF_8)
    val likeRegexPattern = when {
        pattern.isEmpty() -> Pattern.compile("")
        else -> parsePattern(pattern, null)
    }
    when (matchRegexPattern(value, likeRegexPattern)) {
        true -> Datum.bool(true)
        else -> Datum.bool(false)
    }
}
