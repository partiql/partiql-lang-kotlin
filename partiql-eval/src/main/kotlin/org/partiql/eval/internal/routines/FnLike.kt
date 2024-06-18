// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.utils.PatternUtils.matchRegexPattern
import org.partiql.spi.connector.sql.utils.PatternUtils.parsePattern
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.PType.Kind.CLOB
import org.partiql.value.PType.Kind.STRING
import org.partiql.value.PType.Kind.SYMBOL
import org.partiql.value.TextValue
import org.partiql.value.boolValue
import org.partiql.value.check
import java.util.regex.Pattern


internal object Fn_LIKE__STRING_STRING__BOOL : Routine {

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

    override fun invoke(args: Array<Datum>): Datum {
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


internal object Fn_LIKE__SYMBOL_SYMBOL__BOOL : Routine {

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

    override fun invoke(args: Array<Datum>): Datum {
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


internal object Fn_LIKE__CLOB_CLOB__BOOL : Routine {

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

    override fun invoke(args: Array<Datum>): Datum {
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
