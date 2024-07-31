package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelJoin : Rel {

    public fun getLeft(): Rel

    public fun getRight(): Rel

    public fun getCondition(): Rex

    public fun getType(): RelJoinType

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitRelJoin(this, ctx)
}
