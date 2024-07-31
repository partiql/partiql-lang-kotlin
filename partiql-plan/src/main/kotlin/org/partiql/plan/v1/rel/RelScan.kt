package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelScan : Rel {

    public fun getInput(): Rel

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitRelScan(this, ctx)
}
