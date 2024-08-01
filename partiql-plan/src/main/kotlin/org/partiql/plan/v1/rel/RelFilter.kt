package org.partiql.plan.v1.rel

import org.partiql.plan.v1.Schema
import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelFilter : Rel {

    public fun getInput(): Rel

    public fun getPredicate(): Rex

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun getSchema(): Schema = getInput().getSchema()

    override fun isOrdered(): Boolean = getInput().isOrdered()

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitFilter(this, ctx)
}
