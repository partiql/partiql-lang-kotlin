package org.partiql.shape.constraints

import org.partiql.shape.PShape
import org.partiql.value.PartiQLType

public sealed interface Constraint {

    /**
     * Some Constraints require a specific type.
     */
    public fun validate(type: PartiQLType): PShape.ValidationResult
}