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

    override fun getInputs(): List<Rel> = listOf(getLeft(), getRight())

    override fun isOrdered(): Boolean = false

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitJoin(this, ctx)
}
