package org.partiql.plan.rel

import org.partiql.plan.Visitor

/**
 * TODO DELETE ME IN rc2
 */
public class RelError(public val message: String) : Rel {

    override fun getChildren(): Collection<Rel> = emptyList()

    override fun getType(): RelType {
        TODO("Not yet implemented")
    }

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitError(this, ctx)
}
