// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.FunctionUtils.logicalAnd

internal val Fn_AND__BOOL_BOOL__BOOL = FunctionUtils.hidden(
    name = "and",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("lhs", PType.bool()),
        Parameter("rhs", PType.bool()),
    ),
    isNullCall = false,
    isMissingCall = false,
) { args ->
    logicalAnd(args[0], args[1])
}
