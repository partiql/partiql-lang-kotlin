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
        returns = PType.string(),
        parameters = listOf(
            FnParameter("lhs", PType.string()),
            FnParameter("rhs", PType.string()),
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

internal object Fn_CONCAT__CLOB_CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "concat",
        returns = PType.clob(Int.MAX_VALUE),
        parameters = listOf(
            FnParameter("lhs", PType.clob(Int.MAX_VALUE)),
            FnParameter("rhs", PType.clob(Int.MAX_VALUE)),
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
