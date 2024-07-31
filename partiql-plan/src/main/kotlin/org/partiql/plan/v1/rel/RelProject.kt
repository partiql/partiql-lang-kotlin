package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelProject : Rel {

    public fun getInput(): Rel

    public fun getProjections(): List<Rex>

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitProject(this, ctx)
}
