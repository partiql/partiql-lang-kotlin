package org.partiql.shape

import org.partiql.shape.visitor.ShapeVisitor
import org.partiql.value.PartiQLType

public object None : Constraint {
    override fun validate(type: PartiQLType): PShape.ValidationResult = PShape.ValidationResult.Success

    override fun toString(): String = "NONE"

    override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
        TODO()
    }
}
