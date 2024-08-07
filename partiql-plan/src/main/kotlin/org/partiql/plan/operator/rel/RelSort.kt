package org.partiql.plan.operator.rel

import org.partiql.plan.Schema

/**
 * Logical sort operator.
 */
public interface RelSort : Rel {

    public fun getInput(): Rel

    public fun getCollations(): List<RelCollation>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = true

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitSort(this, ctx)
}

