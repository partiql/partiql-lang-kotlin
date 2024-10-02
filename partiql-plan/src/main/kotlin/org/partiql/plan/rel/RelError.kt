package org.partiql.plan.rel

/**
 * TODO DELETE ME IN rc2
 */
public class RelError(public val message: String) : Rel {

    override fun getChildren(): Collection<Rel> = emptyList()

    override fun getSchema(): org.partiql.plan.Schema {
        TODO("Not yet implemented")
    }

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitError(this, ctx)
}
