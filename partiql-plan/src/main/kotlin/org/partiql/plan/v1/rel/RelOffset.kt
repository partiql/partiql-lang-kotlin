package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
interface RelOffset : Rel {

    public fun getInput(): Rel

    public fun getLimit(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitOffset(this, ctx)
}
