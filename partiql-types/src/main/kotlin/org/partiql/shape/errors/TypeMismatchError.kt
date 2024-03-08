package org.partiql.shape.errors

import org.partiql.value.PartiQLType

public class TypeMismatchError(
    public val expected: PartiQLType,
    public val actual: PartiQLType
) : Error {
    override val cause: Throwable? = null
    override val message: String = "Expected $expected, but received $actual."
}
