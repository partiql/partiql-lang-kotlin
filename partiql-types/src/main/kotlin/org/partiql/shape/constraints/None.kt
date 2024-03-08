package org.partiql.shape.constraints

import org.partiql.shape.PShape
import org.partiql.value.PartiQLType

public object None : Constraint {
    override fun validate(type: PartiQLType): PShape.ValidationResult = PShape.ValidationResult.Success
}
