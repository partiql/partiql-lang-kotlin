// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal object Fn_LOWER__STRING__STRING : Function {

    override val signature = FnSignature(
        name = "lower",
        returns = PType.string(),
        parameters = listOf(Parameter("value", PType.string())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.lowercase()
        return Datum.string(result)
    }
}

internal object Fn_LOWER__SYMBOL__SYMBOL : Function {

    override val signature = FnSignature(
        name = "lower",
        returns = PType.symbol(),
        parameters = listOf(Parameter("value", PType.symbol())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.lowercase()
        return Datum.string(result)
    }
}

internal object Fn_LOWER__CLOB__CLOB : Function {

    override val signature = FnSignature(
        name = "lower",
        returns = PType.clob(Int.MAX_VALUE),
        parameters = listOf(Parameter("value", PType.clob(Int.MAX_VALUE))),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.lowercase()
        return Datum.string(result)
    }
}
