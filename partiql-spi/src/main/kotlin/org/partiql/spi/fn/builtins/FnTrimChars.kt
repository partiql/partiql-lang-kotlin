// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.utils.StringUtils.codepointTrim
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_TRIM_CHARS__STRING_STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = PType.string(),
        parameters = listOf(
            FnParameter("value", PType.string()),
            FnParameter("chars", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val chars = args[1].string
        val result = value.codepointTrim(chars)
        return Datum.string(result)
    }
}

internal object Fn_TRIM_CHARS__SYMBOL_SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = PType.symbol(),
        parameters = listOf(
            FnParameter("value", PType.symbol()),
            FnParameter("chars", PType.symbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val chars = args[1].string
        val result = value.codepointTrim(chars)
        return Datum.symbol(result)
    }
}

internal object Fn_TRIM_CHARS__CLOB_CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = PType.clob(Int.MAX_VALUE),
        parameters = listOf(
            FnParameter("value", PType.clob(Int.MAX_VALUE)),
            FnParameter("chars", PType.clob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].bytes.toString(Charsets.UTF_8)
        val chars = args[1].bytes.toString(Charsets.UTF_8)
        val result = string.codepointTrim(chars)
        return Datum.clob(result.toByteArray())
    }
}
