// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

internal object Fn_IS_INT16__ANY__BOOL : Function {

    override val signature = FnSignature(
        name = "is_int16",
        returns = PType.bool(),
        parameters = listOf(Parameter("value", PType.dynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg = args[0]
        return when (arg.type.kind) {
            PType.Kind.TINYINT,
            PType.Kind.SMALLINT -> Datum.bool(true)
            PType.Kind.INTEGER -> {
                val v = arg.int
                Datum.bool(Short.MIN_VALUE <= v && v <= Short.MAX_VALUE)
            }
            PType.Kind.BIGINT -> {
                val v = arg.long
                Datum.bool(Short.MIN_VALUE <= v && v <= Short.MAX_VALUE)
            }
            PType.Kind.NUMERIC -> {
                val v = arg.bigInteger
                return try {
                    v.shortValueExact()
                    Datum.bool(true)
                } catch (_: ArithmeticException) {
                    Datum.bool(false)
                }
            }
            else -> Datum.bool(false)
        }
    }
}
