package org.partiql.shape.constraints

import org.partiql.shape.PShape
import org.partiql.shape.errors.TypeMismatchError
import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.PartiQLType

public data class Element(
    val shape: PShape
) : Constraint {
    override fun validate(type: PartiQLType): PShape.ValidationResult {
        return when (type is BagType || type is ArrayType) {
            true -> PShape.ValidationResult.Success
            false -> PShape.ValidationResult.Failure(
                TypeMismatchError(setOf(BagType(), ArrayType()), type)
            )
        }
    }
}
