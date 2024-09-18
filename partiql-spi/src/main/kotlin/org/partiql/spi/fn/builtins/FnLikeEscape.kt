// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.fn.utils.PatternUtils
import org.partiql.spi.fn.utils.PatternUtils.checkPattern
import org.partiql.spi.fn.utils.PatternUtils.parsePattern
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.util.regex.Pattern

internal object Fn_LIKE_ESCAPE__STRING_STRING_STRING__BOOL : Function {

    override val signature = FnSignature(
        name = "like_escape",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.string()),
            Parameter("pattern", PType.string()),
            Parameter("escape", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val pattern = args[1].string
        val escape = args[2].string
        val (patternString, escapeChar) =
            try {
                checkPattern(pattern, escape)
            } catch (e: IllegalStateException) {
                throw TypeCheckException()
            }
        val likeRegexPattern = when {
            patternString.isEmpty() -> Pattern.compile("")
            else -> parsePattern(patternString, escapeChar)
        }
        return when (PatternUtils.matchRegexPattern(value, likeRegexPattern)) {
            true -> Datum.bool(true)
            else -> Datum.bool(false)
        }
    }
}

internal object Fn_LIKE_ESCAPE__SYMBOL_SYMBOL_SYMBOL__BOOL : Function {

    override val signature = FnSignature(
        name = "like_escape",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.symbol()),
            Parameter("pattern", PType.symbol()),
            Parameter("escape", PType.symbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val pattern = args[1].string
        val escape = args[2].string
        val (patternString, escapeChar) =
            try {
                checkPattern(pattern, escape)
            } catch (e: IllegalStateException) {
                throw TypeCheckException()
            }
        val likeRegexPattern = when {
            patternString.isEmpty() -> Pattern.compile("")
            else -> parsePattern(patternString, escapeChar)
        }
        return when (PatternUtils.matchRegexPattern(value, likeRegexPattern)) {
            true -> Datum.bool(true)
            else -> Datum.bool(false)
        }
    }
}

internal object Fn_LIKE_ESCAPE__CLOB_CLOB_CLOB__BOOL : Function {

    override val signature = FnSignature(
        name = "like_escape",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.clob(Int.MAX_VALUE)),
            Parameter("pattern", PType.clob(Int.MAX_VALUE)),
            Parameter("escape", PType.clob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes.toString(Charsets.UTF_8)
        val pattern = args[1].bytes.toString(Charsets.UTF_8)
        val escape = args[2].bytes.toString(Charsets.UTF_8)
        val (patternString, escapeChar) =
            try {
                checkPattern(pattern, escape)
            } catch (e: IllegalStateException) {
                throw TypeCheckException()
            }
        val likeRegexPattern = when {
            patternString.isEmpty() -> Pattern.compile("")
            else -> parsePattern(patternString, escapeChar)
        }
        return when (PatternUtils.matchRegexPattern(value, likeRegexPattern)) {
            true -> Datum.bool(true)
            else -> Datum.bool(false)
        }
    }
}
