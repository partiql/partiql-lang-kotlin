// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_LOWER__STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "lower",
        returns = PType.typeString(),
        parameters = listOf(FnParameter("value", PType.typeString())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.lowercase()
        return Datum.string(result)
    }
}

internal object Fn_LOWER__SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "lower",
        returns = PType.typeSymbol(),
        parameters = listOf(FnParameter("value", PType.typeSymbol())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.lowercase()
        return Datum.string(result)
    }
}

internal object Fn_LOWER__CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "lower",
        returns = PType.typeClob(Int.MAX_VALUE),
        parameters = listOf(FnParameter("value", PType.typeClob(Int.MAX_VALUE))),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.lowercase()
        return Datum.string(result)
    }
}
