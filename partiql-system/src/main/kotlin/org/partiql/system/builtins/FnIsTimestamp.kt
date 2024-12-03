// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_TIMESTAMP__ANY__BOOL = Function.static(

    name = "is_timestamp",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    val argKind = args[0].type.code()
    Datum.bool(argKind == PType.TIMESTAMPZ || argKind == PType.TIMESTAMP)
}

internal val Fn_IS_TIMESTAMP__BOOL_INT32_ANY__BOOL = Function.static(

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
