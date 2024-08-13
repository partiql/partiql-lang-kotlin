// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_FLOAT32__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_float32",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg = args[0]
        return when (arg.type.kind) {
            PType.Kind.REAL -> Datum.bool(true)
            PType.Kind.DOUBLE -> {
                val v = arg.double
                Datum.bool(Float.MIN_VALUE <= v && v <= Float.MAX_VALUE)
            }
            else -> Datum.bool(false)
        }
    }
}
