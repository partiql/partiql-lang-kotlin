package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexNullIf : Rex {

    public fun getValue(): Rex

    public fun getNullifier(): Rex

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexNullIf(this, ctx)
}
