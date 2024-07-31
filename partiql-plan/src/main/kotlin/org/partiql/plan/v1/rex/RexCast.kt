package org.partiql.plan.v1.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexCast : Rex {

    public fun getOperand(): Rex

    public fun getTarget(): PType

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexCast(this, ctx)
}
