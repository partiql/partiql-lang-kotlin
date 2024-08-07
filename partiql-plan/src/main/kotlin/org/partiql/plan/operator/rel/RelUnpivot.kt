package org.partiql.plan.operator.rel

import org.partiql.plan.operator.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelUnpivot : Rel {

    public fun getInput(): Rex

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitUnpivot(this, ctx)
}
