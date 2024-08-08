// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.utils.StringUtils.codepointTrim
import org.partiql.types.PType

internal object Fn_TRIM_CHARS__STRING_STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = PType.typeString(),
        parameters = listOf(
            FnParameter("value", PType.typeString()),
            FnParameter("chars", PType.typeString()),
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
        returns = PType.typeSymbol(),
        parameters = listOf(
            FnParameter("value", PType.typeSymbol()),
            FnParameter("chars", PType.typeSymbol()),
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
        returns = PType.typeClob(Int.MAX_VALUE),
        parameters = listOf(
            FnParameter("value", PType.typeClob(Int.MAX_VALUE)),
            FnParameter("chars", PType.typeClob(Int.MAX_VALUE)),
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
