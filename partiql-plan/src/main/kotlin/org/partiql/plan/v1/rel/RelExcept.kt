package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelExcept : Rel {

    public fun isAll(): Boolean

    public fun getLeft(): Rel

    public fun getRight(): Rel

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitExcept(this, ctx)
}
