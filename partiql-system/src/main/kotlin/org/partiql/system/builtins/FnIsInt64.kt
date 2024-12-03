// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_INT64__ANY__BOOL = Function.static(

    name = "is_int64",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    val arg = args[0]
    when (arg.type.code()) {
        PType.TINYINT,
        PType.SMALLINT,
        PType.INTEGER,
        PType.BIGINT,
        -> Datum.bool(true)
        PType.NUMERIC -> {
            val v = arg.bigInteger
            try {
                v.longValueExact()
                Datum.bool(true)
            } catch (_: ArithmeticException) {
                Datum.bool(false)
            }
        }
        else -> Datum.bool(false)
    }
}
