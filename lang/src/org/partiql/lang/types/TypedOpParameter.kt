package org.partiql.lang.types

import org.partiql.lang.eval.ExprValue

/**
 * Represents a parameter that can be passed to typed operators i.e CAST/IS
 *
 * @param staticType [StaticType] of this parameter.
 * @param validationThunk thunk that validates if [ExprValue] is valid for this parameter.
 *   If null, all [ExprValue]s that conform to [staticType] are valid.
 */
data class TypedOpParameter(
    val staticType: StaticType,
    val validationThunk: ((ExprValue) -> Boolean)? = null
)