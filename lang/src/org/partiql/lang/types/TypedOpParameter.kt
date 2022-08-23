package org.partiql.lang.types

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue

/**
 * Represents a parameter that can be passed to typed operators i.e CAST/IS
 *
 * Will be removed as we remove [PartiqlPhysical.Type.CustomType] and refactor custom type API later.
 *
 * @param staticType [StaticType] the corresponding static type of this custom type.
 * @param validationThunk how does this custom type validate a value.
 *   If null, all [ExprValue]s that conform to [staticType] are valid.
 */
data class TypedOpParameter(
    val staticType: StaticType,
    val validationThunk: ((ExprValue) -> Boolean)? = null
)
