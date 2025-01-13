// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_IS_INT32__ANY__BOOL = FunctionUtils.hidden(

    name = "is_int32",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    val arg = args[0]
    when (arg.type.code()) {
        PType.TINYINT,
        PType.SMALLINT,
        PType.INTEGER,
        -> Datum.bool(true)
        PType.BIGINT -> {
            val v = arg
            Datum.bool(Integer.MIN_VALUE <= v.long && v.long <= Integer.MAX_VALUE)
        }
        PType.NUMERIC -> {
            val v = arg.bigDecimal
            try {
                v.intValueExact()
                Datum.bool(true)
            } catch (_: ArithmeticException) {
                Datum.bool(false)
            }
        }
        else -> Datum.bool(false)
    }
}
