// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_CHAR__ANY__BOOL = Function.static(
    name = "is_char",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),
) { args ->
    Datum.bool(args[0].type.kind == PType.Kind.CHAR)
}

private val TEXT_TYPES_WITH_LENGTH = setOf(
    PType.Kind.CHAR,
    PType.Kind.VARCHAR
)

internal val Fn_IS_CHAR__INT32_ANY__BOOL = Function.static(
    name = "is_char",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("type_parameter_1", PType.integer()),
        Parameter("value", PType.dynamic()),
    ),
) { args ->
    val value = args[0]
    if (value.type.kind in TEXT_TYPES_WITH_LENGTH) {
        Datum.bool(false)
    }
    val length = args[0].int
    if (length < 0) {
        throw TypeCheckException()
    }
    Datum.bool(value.type.length == length)
}
