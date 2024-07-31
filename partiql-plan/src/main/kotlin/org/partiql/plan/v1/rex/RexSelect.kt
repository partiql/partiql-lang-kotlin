package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * TODO DOCUMENTATION
 */
public interface RexSelect : Rex {

    public fun getInput(): Rel

    public fun getConstructor(): Rex

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexSelect(this, ctx)
}
