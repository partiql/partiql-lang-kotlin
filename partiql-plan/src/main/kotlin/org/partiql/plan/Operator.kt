package org.partiql.plan

/**
 * Operator is the interface for a logical plan operator.
 */
public interface Operator {

    public fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R

    /**
     * TODO make `getInputs()` and add `getInput(int index)`
     *
     * @return
     */
    public fun getChildren(): Collection<Operator>
}
