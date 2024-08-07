package org.partiql.plan.operator.rel

/**
 * TODO GROUP STRATEGY
 * TODO GROUP BY
 */
public interface RelAggregate : Rel {

    public fun getInput(): Rel

    public fun getCalls(): List<RelAggregateCall>

    override fun getInputs(): List<Rel> = listOf(getInput())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitAggregate(this, ctx)
}
