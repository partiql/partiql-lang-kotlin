// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.planner.internal.fn.utils.PatternUtils.matchRegexPattern
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
internal object Fn_LIKE__STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "like",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("pattern", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TextValue<String>>().value!!
        val pattern = args[1].check<TextValue<String>>().value!!
        val likeRegexPattern = when {
            pattern.isEmpty() -> Pattern.compile("")
            else -> parsePattern(pattern, null)
        }
        return when (matchRegexPattern(value, likeRegexPattern)) {
            true -> boolValue(true)
            else -> boolValue(false)
        }
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LIKE__SYMBOL_SYMBOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "like",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("pattern", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TextValue<String>>().value!!
        val pattern = args[1].check<TextValue<String>>().value!!
        val likeRegexPattern = when {
            pattern.isEmpty() -> Pattern.compile("")
            else -> parsePattern(pattern, null)
        }
        return when (matchRegexPattern(value, likeRegexPattern)) {
            true -> boolValue(true)
            else -> boolValue(false)
        }
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_LIKE__CLOB_CLOB__BOOL : Fn {

    override val signature = FnSignature(
        name = "like",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("pattern", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val pattern = args[1].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val likeRegexPattern = when {
            pattern.isEmpty() -> Pattern.compile("")
            else -> parsePattern(pattern, null)
        }
        return when (matchRegexPattern(value, likeRegexPattern)) {
            true -> boolValue(true)
            else -> boolValue(false)
        }
    }
}
