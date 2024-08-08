// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_CHAR_LENGTH__STRING__INT : Fn {

    override val signature = FnSignature(
        name = "char_length",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("value", PType.typeString()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        return Datum.integer(value.codePointCount(0, value.length))
    }
}

internal object Fn_CHAR_LENGTH__SYMBOL__INT : Fn {

    override val signature = FnSignature(
        name = "char_length",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("lhs", PType.typeSymbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].string
        return Datum.integer(value.codePointCount(0, value.length))
    }
}

internal object Fn_CHAR_LENGTH__CLOB__INT : Fn {

    override val signature = FnSignature(
        name = "char_length",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("lhs", PType.typeClob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bytes
        return Datum.integer(value.size)
    }
}
