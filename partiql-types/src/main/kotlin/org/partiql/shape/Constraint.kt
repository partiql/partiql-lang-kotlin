package org.partiql.shape

import org.partiql.value.PartiQLType

public sealed interface Constraint : ShapeNode {

    /**
     * Some Constraints require a specific type.
     */
    public fun validate(type: PartiQLType): PShape.ValidationResult
}
