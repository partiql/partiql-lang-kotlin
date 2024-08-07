package org.partiql.plan.operator.rel

/**
 * Logical `EXCLUDE` operation.
 */
public interface RelExclude : Rel {

    public fun getInput(): Rel

    public fun getPaths(): List<RelExcludePath>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = getInput().isOrdered()

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitExclude(this, ctx)
}
