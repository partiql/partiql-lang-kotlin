package org.partiql.plan.operator.rel

import org.partiql.plan.operator.rex.Rex

/**
 * Logical `PROJECTION` operator
 */
public interface RelProject : Rel {

    public fun getInput(): Rel

    public fun getProjections(): List<Rex>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitProject(this, ctx)
}
