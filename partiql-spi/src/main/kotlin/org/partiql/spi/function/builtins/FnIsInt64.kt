// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

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
    when (arg.type.kind) {
        PType.Kind.TINYINT,
        PType.Kind.SMALLINT,
        PType.Kind.INTEGER,
        PType.Kind.BIGINT,
        -> Datum.bool(true)
        PType.Kind.NUMERIC -> {
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
