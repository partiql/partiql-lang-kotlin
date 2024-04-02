package org.partiql.shape.errors

import org.partiql.value.PartiQLType

public class TypeMismatchError(
    public val expected: Set<PartiQLType>,
    public val actual: PartiQLType
) : Error {
    override val cause: Throwable? = null
    override val message: String = "Expected one of $expected, but received $actual."

    public constructor(expected: PartiQLType, actual: PartiQLType) : this(setOf(expected), actual)
}
