// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_LOWER__STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "lower",
        returns = PType.string(),
        parameters = listOf(FnParameter("value", PType.string())),
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
        returns = PType.symbol(),
        parameters = listOf(FnParameter("value", PType.symbol())),
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
        returns = PType.clob(Int.MAX_VALUE),
        parameters = listOf(FnParameter("value", PType.clob(Int.MAX_VALUE))),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.lowercase()
        return Datum.string(result)
    }
}
