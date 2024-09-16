// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.types.PType

internal object Fn_UPPER__STRING__STRING : Function {

    override val signature = FnSignature(
        name = "upper",
        returns = PType.string(),
        parameters = listOf(Parameter("value", PType.string())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.uppercase()
        return Datum.string(result)
    }
}

internal object Fn_UPPER__SYMBOL__SYMBOL : Function {

    override val signature = FnSignature(
        name = "upper",
        returns = PType.symbol(),
        parameters = listOf(Parameter("value", PType.symbol())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].string
        val result = string.uppercase()
        return Datum.symbol(result)
    }
}

internal object Fn_UPPER__CLOB__CLOB : Function {

    override val signature = FnSignature(
        name = "upper",
        returns = PType.clob(Int.MAX_VALUE),
        parameters = listOf(Parameter("value", PType.clob(Int.MAX_VALUE))),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].bytes.toString(Charsets.UTF_8)
        val result = string.uppercase()
        return Datum.clob(result.toByteArray())
    }
}
