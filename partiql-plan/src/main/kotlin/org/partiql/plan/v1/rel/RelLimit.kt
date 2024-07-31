package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
interface RelLimit : Rel {

    public fun getInput(): Rel

    public fun getLimit(): Rex

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitLimit(this, ctx)
}
