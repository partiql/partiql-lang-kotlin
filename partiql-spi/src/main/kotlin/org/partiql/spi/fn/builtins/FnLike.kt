// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.utils.PatternUtils.matchRegexPattern
import org.partiql.spi.fn.utils.PatternUtils.parsePattern
import org.partiql.types.PType
import java.util.regex.Pattern

internal object Fn_LIKE__STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "like",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.string()),
            FnParameter("pattern", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val pattern = args[1].string
        val likeRegexPattern = when {
            pattern.isEmpty() -> Pattern.compile("")
            else -> parsePattern(pattern, null)
        }
        return when (matchRegexPattern(value, likeRegexPattern)) {
            true -> Datum.bool(true)
            else -> Datum.bool(false)
        }
    }
}

internal object Fn_LIKE__CLOB_CLOB__BOOL : Fn {

    override val signature = FnSignature(
        name = "like",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.clob(Int.MAX_VALUE)),
            FnParameter("pattern", PType.clob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes.toString(Charsets.UTF_8)
        val pattern = args[1].bytes.toString(Charsets.UTF_8)
        val likeRegexPattern = when {
            pattern.isEmpty() -> Pattern.compile("")
            else -> parsePattern(pattern, null)
        }
        return when (matchRegexPattern(value, likeRegexPattern)) {
            true -> Datum.bool(true)
            else -> Datum.bool(false)
        }
    }
}
