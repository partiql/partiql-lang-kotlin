// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_CHAR_LENGTH__STRING__INT : Function {

    override val signature = FnSignature(
        name = "char_length",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("value", PType.string()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        return Datum.integer(value.codePointCount(0, value.length))
    }
}

internal object Fn_CHAR_LENGTH__SYMBOL__INT : Function {

    override val signature = FnSignature(
        name = "char_length",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("lhs", PType.symbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        return Datum.integer(value.codePointCount(0, value.length))
    }
}

internal object Fn_CHAR_LENGTH__CLOB__INT : Function {

    override val signature = FnSignature(
        name = "char_length",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("lhs", PType.clob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes
        return Datum.integer(value.size)
    }
}
