// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_IS_TIMESTAMP__ANY__BOOL = FunctionUtils.hidden(

    name = "is_timestamp",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    val argKind = args[0].type.code()
    Datum.bool(argKind == PType.TIMESTAMPZ || argKind == PType.TIMESTAMP)
}

internal val Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL = FunctionUtils.hidden(

    name = "is_timestamp",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("type_parameter_1", PType.bool()),
        Parameter("type_parameter_2", PType.integer()),
        Parameter("value", PType.dynamic()),
    ),

) { args ->
    TODO("Function is_timestamp not implemented")
}
