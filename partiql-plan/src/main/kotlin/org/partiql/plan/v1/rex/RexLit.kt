package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexLit : Rex {

    /**
     * TODO REPLACE WITH DATUM
     */
    public fun getValue(): String

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitLit(this, ctx)
}
