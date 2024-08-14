// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType
import org.partiql.types.PType.Kind

internal object Fn_IS_BAG__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_bag",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return Datum.bool(args[0].type.kind == Kind.BAG)
    }
}
