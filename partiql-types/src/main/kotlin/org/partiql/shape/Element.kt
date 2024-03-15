package org.partiql.shape

import org.partiql.shape.errors.TypeMismatchError
import org.partiql.shape.visitor.ShapeVisitor
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
                TypeMismatchError(setOf(BagType, ArrayType), type)
            )
        }
    }

    override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
        return visitor.visitConstraintElement(this, ctx)
    }
}
