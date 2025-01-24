// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

internal val Fn_IS_CHAR__ANY__BOOL = FunctionUtils.hidden(
    name = "is_char",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),
) { args ->
    Datum.bool(args[0].type.code() == PType.CHAR)
}

private val TEXT_TYPES_WITH_LENGTH = setOf(
    PType.CHAR,
    PType.VARCHAR
)

internal val Fn_IS_CHAR__INT32_ANY__BOOL = FunctionUtils.hidden(
    name = "is_char",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("type_parameter_1", PType.integer()),
        Parameter("value", PType.dynamic()),
    ),
) { args ->
    val value = args[0]
    if (value.type.code() in TEXT_TYPES_WITH_LENGTH) {
        Datum.bool(false)
    }
    val length = args[0].int
    if (length < 0) {
        throw PErrors.internalErrorException(IllegalArgumentException("Length must be non-negative."))
    }
    Datum.bool(value.type.length == length)
}
