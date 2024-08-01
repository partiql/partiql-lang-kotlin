package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelSort : Rel {

    public fun getInput(): Rel

    public fun getSortSpecs(): List<RelSortSpec>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = true

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitSort(this, ctx)
}
