// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_FLOAT32__ANY__BOOL = FunctionUtils.hidden(

    name = "is_float32",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    val arg = args[0]
    when (arg.type.code()) {
        PType.REAL -> Datum.bool(true)
        PType.DOUBLE -> {
            val v = arg.double
            Datum.bool(Float.MIN_VALUE <= v && v <= Float.MAX_VALUE)
        }
        else -> Datum.bool(false)
    }
}
