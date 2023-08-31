package org.partiql.types.function

import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Parameter of a FunctionSignature.
 *
 * @property name A human-readable name to help clarify its use.
 * @property type The parameter's PartiQL type.
 */
public data class FunctionParameter(
    public val name: String,
    public val type: PartiQLValueType,
)
