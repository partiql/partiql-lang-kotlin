package org.partiql.plan.operator.rel

import org.partiql.plan.Schema

/**
 * Logical `DISTINCT` operator.
 */
public interface RelDistinct : Rel {

    public fun getInput(): Rel

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitDistinct(this, ctx)
}
