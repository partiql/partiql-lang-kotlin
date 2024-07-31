package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelExclude : Rel {

    public fun getInput(): Rel

    public fun getPaths(): List<RelExcludePath>

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitExclude(this, ctx)
}
