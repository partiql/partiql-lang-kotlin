// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.FunctionUtils.logicalOr

internal val Fn_OR__BOOL_BOOL__BOOL = FunctionUtils.hidden(
    name = "or",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.bool()),
        Parameter("rhs", PType.bool()),
    ),
    isNullCall = false,
    isMissingCall = false,
) { args ->
    logicalOr(args[0], args[1])
}
