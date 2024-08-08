// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_CONCAT__STRING_STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "concat",
        returns = PType.typeString(),
        parameters = listOf(
            FnParameter("lhs", PType.typeString()),
            FnParameter("rhs", PType.typeString()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].string
        val arg1 = args[1].string
        return Datum.string(arg0 + arg1)
    }
}

internal object Fn_CONCAT__SYMBOL_SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "concat",
        returns = PType.typeSymbol(),
        parameters = listOf(
            FnParameter("lhs", PType.typeSymbol()),
            FnParameter("rhs", PType.typeSymbol()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].string
        val arg1 = args[1].string
        return Datum.symbol(arg0 + arg1)
    }
}

internal object Fn_CONCAT__CLOB_CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "concat",
        returns = PType.typeClob(Int.MAX_VALUE),
        parameters = listOf(
            FnParameter("lhs", PType.typeClob(Int.MAX_VALUE)),
            FnParameter("rhs", PType.typeClob(Int.MAX_VALUE)),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].bytes
        val arg1 = args[1].bytes
        return Datum.clob(arg0 + arg1)
    }
}
