package org.partiql.plan.operator.rel

import org.partiql.plan.Schema
import org.partiql.plan.operator.rex.Rex

/**
 * Logical `LIMIT` operator.
 */
public interface RelLimit : Rel {

    public fun getInput(): Rel

    public fun getLimit(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitLimit(this, ctx)
}
