package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexVar : Rex {

    /**
     * 0-indexed scope offset.
     */
    public fun getDepth(): Int

    /**
     * 0-index tuple offset.
     */
    public fun getOffset(): Int

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitVar(this, ctx)
}
