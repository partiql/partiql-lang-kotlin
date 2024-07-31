package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexCoalesce : Rex {

    public fun getArgs(): List<Rex>

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexCoalesce(this, ctx)
}
