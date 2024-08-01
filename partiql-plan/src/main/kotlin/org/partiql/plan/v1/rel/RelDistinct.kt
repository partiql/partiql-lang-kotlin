package org.partiql.plan.v1.rel

import org.partiql.plan.v1.Schema

/**
 * TODO DOCUMENTATION
 */
public interface RelDistinct : Rel {

    public fun getInput(): Rel

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitDistinct(this, ctx)
}
