// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_INT32__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_int32",
        returns = PType.bool(),
        parameters = listOf(FnParameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg = args[0]
        return when (arg.type.kind) {
            PType.Kind.TINYINT,
            PType.Kind.SMALLINT,
            PType.Kind.INTEGER -> Datum.bool(true)
            PType.Kind.BIGINT -> {
                val v = arg
                Datum.bool(Integer.MIN_VALUE <= v.long && v.long <= Integer.MAX_VALUE)
            }
            PType.Kind.NUMERIC -> {
                val v = arg.bigInteger
                return try {
                    v.intValueExact()
                    Datum.bool(true)
                } catch (_: ArithmeticException) {
                    Datum.bool(false)
                }
            }
            else -> Datum.bool(false)
        }
    }
}
