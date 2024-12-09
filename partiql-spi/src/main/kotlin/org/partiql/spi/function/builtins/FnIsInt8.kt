// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_INT8__ANY__BOOL = Function.static(

    name = "is_int8",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    val arg = args[0]
    when (arg.type.code()) {
        PType.TINYINT -> Datum.bool(true)
        PType.SMALLINT -> {
            val v = arg.short
            Datum.bool(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
        }
        PType.INTEGER -> {
            val v = arg.int
            Datum.bool(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
        }
        PType.BIGINT -> {
            val v = arg.long
            Datum.bool(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
        }
        PType.NUMERIC -> {
            val v = arg.bigInteger
            try {
                v.byteValueExact()
                Datum.bool(true)
            } catch (_: ArithmeticException) {
                Datum.bool(false)
            }
        }
        else -> Datum.bool(false)
    }
}
