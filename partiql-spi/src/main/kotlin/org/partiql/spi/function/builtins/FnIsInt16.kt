// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_INT16__ANY__BOOL = Function.static(

    name = "is_int16",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    val arg = args[0]
    when (arg.type.code()) {
        PType.TINYINT,
        PType.SMALLINT,
        -> Datum.bool(true)
        PType.INTEGER -> {
            val v = arg.int
            Datum.bool(Short.MIN_VALUE <= v && v <= Short.MAX_VALUE)
        }
        PType.BIGINT -> {
            val v = arg.long
            Datum.bool(Short.MIN_VALUE <= v && v <= Short.MAX_VALUE)
        }
        PType.NUMERIC -> {
            val v = arg.bigInteger
            try {
                v.shortValueExact()
                Datum.bool(true)
            } catch (_: ArithmeticException) {
                Datum.bool(false)
            }
        }
        else -> Datum.bool(false)
    }
}
