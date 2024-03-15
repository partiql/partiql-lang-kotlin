package org.partiql.shape.visitor

import org.partiql.shape.AnyOf
import org.partiql.shape.Constraint
import org.partiql.shape.Element
import org.partiql.shape.Fields
import org.partiql.shape.NotNull
import org.partiql.shape.PShape
import org.partiql.shape.ShapeNode

public interface ShapeVisitor<R, C> {
    public fun visit(node: ShapeNode, ctx: C): R

    public fun visitShape(node: PShape, ctx: C): R

    public fun visitConstraint(node: Constraint, ctx: C): R

    public fun visitConstraintAnyOf(node: AnyOf, ctx: C): R

    public fun visitConstraintElement(node: Element, ctx: C): R

    public fun visitConstraintFields(node: Fields, ctx: C): R

    public fun visitConstraintNotNull(node: NotNull, ctx: C): R
}
