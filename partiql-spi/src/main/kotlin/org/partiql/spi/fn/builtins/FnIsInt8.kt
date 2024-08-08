// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_IS_INT8__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_int8",
        returns = PType.typeBool(),
        parameters = listOf(FnParameter("value", PType.typeDynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg = args[0]
        return when (arg.type.kind) {
            PType.Kind.TINYINT -> Datum.bool(true)
            PType.Kind.SMALLINT -> {
                val v = arg.short
                Datum.bool(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            PType.Kind.INT -> {
                val v = arg.int
                Datum.bool(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            PType.Kind.BIGINT -> {
                val v = arg.long
                Datum.bool(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            PType.Kind.INT_ARBITRARY -> {
                val v = arg.bigInteger
                return try {
                    v.byteValueExact()
                    Datum.bool(true)
                } catch (_: ArithmeticException) {
                    Datum.bool(false)
                }
            }
            else -> Datum.bool(false)
        }
    }
}
