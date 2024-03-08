package org.partiql.shape.constraints

import org.partiql.shape.PShape
import org.partiql.value.PartiQLType

/**
 * SQL:1999's NOT NULL
 */
public object NotNull : Constraint {
    override fun validate(type: PartiQLType): PShape.ValidationResult {
        return PShape.ValidationResult.Success
    }
}
