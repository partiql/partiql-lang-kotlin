// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.planner.internal.fn.utils.PatternUtils
import org.partiql.planner.internal.fn.utils.PatternUtils.checkPattern
import org.partiql.planner.internal.fn.utils.PatternUtils.parsePattern
import org.partiql.value.ClobValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.TextValue
import org.partiql.value.boolValue
import org.partiql.value.check
import java.util.regex.Pattern

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LIKE_ESCAPE__STRING_STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "like_escape",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("pattern", STRING),
            FnParameter("escape", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TextValue<String>>().value!!
        val pattern = args[1].check<TextValue<String>>().value!!
        val escape = args[2].check<TextValue<String>>().value!!
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
            true -> boolValue(true)
            else -> boolValue(false)
        }
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LIKE_ESCAPE__SYMBOL_SYMBOL_SYMBOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "like_escape",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("pattern", SYMBOL),
            FnParameter("escape", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TextValue<String>>().value!!
        val pattern = args[1].check<TextValue<String>>().value!!
        val escape = args[2].check<TextValue<String>>().value!!
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
            true -> boolValue(true)
            else -> boolValue(false)
        }
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LIKE_ESCAPE__CLOB_CLOB_CLOB__BOOL : Fn {

    override val signature = FnSignature(
        name = "like_escape",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("pattern", CLOB),
            FnParameter("escape", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val pattern = args[1].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val escape = args[2].check<ClobValue>().value!!.toString(Charsets.UTF_8)
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
            true -> boolValue(true)
            else -> boolValue(false)
        }
    }
}
