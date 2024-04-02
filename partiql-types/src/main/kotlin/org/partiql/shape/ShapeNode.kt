package org.partiql.shape

import org.partiql.shape.visitor.ShapeVisitor

public sealed interface ShapeNode {

    public fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R
}
