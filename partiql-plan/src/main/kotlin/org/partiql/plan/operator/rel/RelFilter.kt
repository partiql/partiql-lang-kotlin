package org.partiql.plan.operator.rel

import org.partiql.plan.Schema
import org.partiql.plan.operator.rex.Rex

/**
 * Logical filter operation for the WHERE and HAVING clauses.
 */
public interface RelFilter : Rel {

    public fun getInput(): Rel

    public fun getPredicate(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitFilter(this, ctx)
}
