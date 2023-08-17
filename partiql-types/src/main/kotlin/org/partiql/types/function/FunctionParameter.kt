package org.partiql.types.function

import org.partiql.types.PartiQLValueType

public sealed class FunctionParameter(
    public val name: String,
    public val type: PartiQLValueType,
) {

    public class V(name: String, type: PartiQLValueType) : FunctionParameter(name, type)

    public class T(name: String, type: PartiQLValueType) : FunctionParameter(name, type)
}
