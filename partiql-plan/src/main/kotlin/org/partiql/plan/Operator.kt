package org.partiql.plan

/**
 * Operator is the interface for a logical plan operator.
 */
public interface Operator {

    public fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R

    public fun getChildren(): Collection<Operator>
}
