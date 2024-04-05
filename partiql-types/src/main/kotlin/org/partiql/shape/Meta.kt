package org.partiql.shape

import org.partiql.shape.visitor.ShapeVisitor

/**
 * Empty interface that allows
 */
public interface Meta : ShapeNode {
    public abstract class Base : Meta {
        override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
            return visitor.visitMeta(this, ctx)
        }
    }
}
