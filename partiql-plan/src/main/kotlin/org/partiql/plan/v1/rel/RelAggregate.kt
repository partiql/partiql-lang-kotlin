package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelAggregate : Rel {

    public fun getInput(): Rel

    public fun getCalls(): RelAggregateCall

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = false

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitAggregate(this, ctx)
}
