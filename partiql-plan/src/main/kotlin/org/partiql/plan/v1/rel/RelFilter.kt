package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelFilter : Rel {

    public fun getInput(): Rel

    public fun getPredicate(): Rex

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitFilter(this, ctx)
}
