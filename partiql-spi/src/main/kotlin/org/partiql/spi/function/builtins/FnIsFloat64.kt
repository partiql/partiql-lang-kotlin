// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_IS_FLOAT64__ANY__BOOL = FunctionUtils.hidden(

    name = "is_float64",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    when (args[0].type.code()) {
        PType.REAL,
        PType.DOUBLE,
        -> Datum.bool(true)
        else -> Datum.bool(false)
    }
}
