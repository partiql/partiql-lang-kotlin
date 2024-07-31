package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelAggregate : Rel {

    public fun getInput(): Rel

    public fun getCalls(): RelAggregateCall

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitAggregate(this, ctx)
}
