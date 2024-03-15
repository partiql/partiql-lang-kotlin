package org.partiql.shape

import org.partiql.shape.visitor.ShapeVisitor
import org.partiql.value.PartiQLType

public data class AnyOf(
    val shapes: Set<PShape>
) : Constraint {

    override fun validate(type: PartiQLType): PShape.ValidationResult {
        TODO("Not yet implemented")
    }

    override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
        return visitor.visitConstraintAnyOf(this, ctx)
    }
}
