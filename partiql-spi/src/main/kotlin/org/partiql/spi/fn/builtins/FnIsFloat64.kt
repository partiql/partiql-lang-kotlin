// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_FLOAT64__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_float64",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return when (args[0].type.kind) {
            PType.Kind.REAL,
            PType.Kind.DOUBLE -> Datum.bool(true)
            else -> Datum.bool(false)
        }
    }
}
