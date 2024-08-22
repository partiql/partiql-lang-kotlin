package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema

/**
 * TODO DELETE ME
 */
public class RelError(val message: String) : Rel {

    override fun getChildren(): Collection<Rel> = emptyList()

    override fun getSchema(): Schema {
        TODO("Not yet implemented")
    }

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitError(this, ctx)
}
