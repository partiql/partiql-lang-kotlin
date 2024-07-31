package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 */
public interface RelAggregate : Rel {

    public fun getInput(): Rel

    public fun getCalls(): RelAggregateCall
}
