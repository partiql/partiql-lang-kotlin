// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.PatternUtils
import org.partiql.spi.utils.PatternUtils.checkPattern
import org.partiql.spi.utils.PatternUtils.parsePattern
import org.partiql.spi.value.Datum
import java.util.regex.Pattern

internal val Fn_LIKE_ESCAPE__STRING_STRING_STRING__BOOL = FunctionUtils.hidden(

    name = "like_escape",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
        Parameter("pattern", PType.string()),
        Parameter("escape", PType.string()),
    ),

) { args ->
    val value = args[0].string
    val pattern = args[1].string
    val escape = args[2].string
    val (patternString, escapeChar) =
        try {
            checkPattern(pattern, escape)
        } catch (e: IllegalStateException) {
            throw PErrors.internalErrorException(e)
        }
    val likeRegexPattern = when {
        patternString.isEmpty() -> Pattern.compile("")
        else -> parsePattern(patternString, escapeChar)
    }
    when (PatternUtils.matchRegexPattern(value, likeRegexPattern)) {
        true -> Datum.bool(true)
        else -> Datum.bool(false)
    }
}

internal val Fn_LIKE_ESCAPE__CLOB_CLOB_CLOB__BOOL = FunctionUtils.hidden(

    name = "like_escape",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.clob(Long.MAX_VALUE)),
        Parameter("pattern", PType.clob(Long.MAX_VALUE)),
        Parameter("escape", PType.clob(Long.MAX_VALUE)),
    ),

) { args ->
    val value = args[0].bytes.toString(Charsets.UTF_8)
    val pattern = args[1].bytes.toString(Charsets.UTF_8)
    val escape = args[2].bytes.toString(Charsets.UTF_8)
    val (patternString, escapeChar) =
        try {
            checkPattern(pattern, escape)
        } catch (e: IllegalStateException) {
            throw PErrors.internalErrorException(e)
        }
    val likeRegexPattern = when {
        patternString.isEmpty() -> Pattern.compile("")
        else -> parsePattern(patternString, escapeChar)
    }
    when (PatternUtils.matchRegexPattern(value, likeRegexPattern)) {
        true -> Datum.bool(true)
        else -> Datum.bool(false)
    }
}
