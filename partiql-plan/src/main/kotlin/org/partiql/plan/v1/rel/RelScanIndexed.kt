package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelScanIndexed : Rel {

    public fun getInput(): Rel

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = true

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitScanIndexed(this, ctx)
}
