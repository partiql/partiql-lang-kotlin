package org.partiql.shape

import org.partiql.shape.visitor.ShapeVisitor
import org.partiql.value.PartiQLType

/**
 * SQL:1999's NOT NULL
 */
public object NotNull : Constraint {
    override fun validate(type: PartiQLType): PShape.ValidationResult {
        return PShape.ValidationResult.Success
    }

    override fun toString(): String = "NOT_NULL"

    override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
        return visitor.visitConstraintNotNull(this, ctx)
    }
}
