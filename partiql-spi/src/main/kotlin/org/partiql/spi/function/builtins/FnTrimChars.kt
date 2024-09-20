// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.StringUtils.codepointTrim
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_TRIM_CHARS__STRING_STRING__STRING : Function {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = PType.string(),
        parameters = listOf(
            Parameter("value", PType.string()),
            Parameter("chars", PType.string()),
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

internal object Fn_TRIM_CHARS__SYMBOL_SYMBOL__SYMBOL : Function {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = PType.symbol(),
        parameters = listOf(
            Parameter("value", PType.symbol()),
            Parameter("chars", PType.symbol()),
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

internal object Fn_TRIM_CHARS__CLOB_CLOB__CLOB : Function {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = PType.clob(Int.MAX_VALUE),
        parameters = listOf(
            Parameter("value", PType.clob(Int.MAX_VALUE)),
            Parameter("chars", PType.clob(Int.MAX_VALUE)),
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
