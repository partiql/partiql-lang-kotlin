package org.partiql.shape.visitor

import org.partiql.shape.AnyOf
import org.partiql.shape.Constraint
import org.partiql.shape.Element
import org.partiql.shape.Fields
import org.partiql.shape.Meta
import org.partiql.shape.Multiple
import org.partiql.shape.None
import org.partiql.shape.NotNull
import org.partiql.shape.PShape
import org.partiql.shape.ShapeNode

public abstract class ShapeBaseVisitor<R, C> : ShapeVisitor<R, C> {

    public abstract fun defaultVisit(node: ShapeNode, ctx: C): R

    override fun visit(node: ShapeNode, ctx: C): R = node.accept(this, ctx)

    override fun visitConstraint(node: Constraint, ctx: C): R = when (node) {
        is AnyOf -> visitConstraintAnyOf(node, ctx)
        is Element -> visitConstraintElement(node, ctx)
        is Fields -> visitConstraintFields(node, ctx)
        is NotNull -> visitConstraintNotNull(node, ctx)
        is Multiple -> TODO("Remove")
        None -> TODO("Remove")
    }

    override fun visitConstraintElement(node: Element, ctx: C): R = defaultVisit(node, ctx)

    override fun visitConstraintAnyOf(node: AnyOf, ctx: C): R = defaultVisit(node, ctx)

    override fun visitConstraintFields(node: Fields, ctx: C): R = defaultVisit(node, ctx)

    override fun visitConstraintNotNull(node: NotNull, ctx: C): R = defaultVisit(node, ctx)

    override fun visitShape(node: PShape, ctx: C): R = defaultVisit(node, ctx)

    override fun visitMeta(node: Meta, ctx: C): R = defaultVisit(node, ctx)
}
