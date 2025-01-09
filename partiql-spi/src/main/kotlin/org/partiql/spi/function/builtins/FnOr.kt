// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

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
    val lhs = args[0]
    val rhs = args[1]
    val lhsIsUnknown = lhs.isNull || lhs.isMissing
    val rhsIsUnknown = rhs.isNull || rhs.isMissing

    // SQL:1999 Section 6.30 Table 13
    when {
        lhsIsUnknown && rhsIsUnknown -> Datum.nullValue(PType.bool())
        !lhsIsUnknown && !rhsIsUnknown -> Datum.bool(lhs.boolean || rhs.boolean)
        lhsIsUnknown && rhs.boolean -> Datum.bool(true)
        rhsIsUnknown && lhs.boolean -> Datum.bool(true)
        else -> Datum.nullValue(PType.bool())
    }
}
