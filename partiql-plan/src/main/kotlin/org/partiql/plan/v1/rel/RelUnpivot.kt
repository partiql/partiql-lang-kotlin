package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelUnpivot : Rel {

    public fun getInput(): Rex

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = false

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitUnpivot(this, ctx)
}
