// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_STRING__ANY__BOOL = FunctionUtils.hidden(
    name = "is_string",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    Datum.bool(args[0].type.code() == PType.STRING)
}

internal val Fn_IS_STRING__INT32_ANY__BOOL = FunctionUtils.hidden(
    name = "is_string",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("type_parameter_1", PType.integer()),
        Parameter("value", PType.dynamic()),
    ),
) { args ->
    val v = args[1]
    if (v.type.code() != PType.STRING) {
        return@hidden Datum.bool(false)
    }
    val length = args[0].int
    if (length < 0) {
        throw TypeCheckException()
    }
    Datum.bool(v.string.length <= length)
}
