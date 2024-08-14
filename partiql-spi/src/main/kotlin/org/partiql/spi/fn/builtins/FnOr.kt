// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_OR__BOOL_BOOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "or",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("lhs", PType.bool()),
            FnParameter("rhs", PType.bool()),
        ),
        isNullable = true,
        isNullCall = false,
        isMissable = false,
        isMissingCall = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        if (args[0].isNull || args[1].isNull) return Datum.nullValue(PType.bool())
        val lhs = args[0].boolean
        val rhs = args[1].boolean
        return Datum.bool(lhs || rhs)
    }
}
