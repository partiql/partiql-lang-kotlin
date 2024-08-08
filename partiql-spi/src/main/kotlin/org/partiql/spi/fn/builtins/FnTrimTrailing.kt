// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.utils.StringUtils.codepointTrimTrailing
import org.partiql.types.PType

internal object Fn_TRIM_TRAILING__STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "trim_trailing",
        returns = PType.typeString(),
        parameters = listOf(FnParameter("value", PType.typeString())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val result = value.codepointTrimTrailing()
        return Datum.string(result)
    }
}

internal object Fn_TRIM_TRAILING__SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "trim_trailing",
        returns = PType.typeSymbol(),
        parameters = listOf(FnParameter("value", PType.typeSymbol())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        val result = value.codepointTrimTrailing()
        return Datum.symbol(result)
    }
}

internal object Fn_TRIM_TRAILING__CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "trim_trailing",
        returns = PType.typeClob(Int.MAX_VALUE),
        parameters = listOf(FnParameter("value", PType.typeClob(Int.MAX_VALUE))),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].bytes.toString(Charsets.UTF_8)
        val result = string.codepointTrimTrailing()
        return Datum.clob(result.toByteArray())
    }
}
