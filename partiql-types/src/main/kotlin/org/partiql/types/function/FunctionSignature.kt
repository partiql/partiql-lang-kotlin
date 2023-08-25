package org.partiql.types.function

import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * Represents the signature of a PartiQL function.
 *
 * The signature includes the names of the function (which allows for function overloading),
 * the return type, a list of parameters, a flag indicating whether the function is deterministic
 * (i.e., always produces the same output given the same input), and an optional description.
 */
public class FunctionSignature @OptIn(PartiQLValueExperimental::class) constructor(
    public val name: String,
    public val returns: PartiQLValueType,
    public val parameters: List<FunctionParameter> = emptyList(),
    public val isDeterministic: Boolean = true,
    public val description: String? = null
)
