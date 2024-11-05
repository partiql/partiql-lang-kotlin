package org.partiql.plan

/**
 * Operator is the interface for a logical plan operator.
 */
public interface Operator {

    public fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R

    /**
     * Get i-th child (input) operator.
     *
     * @param index
     */
    public fun getChild(index: Int) {
        throw UnsupportedOperationException("getChild")
    }

    /**
     * Get all child (input) operators.
     *
     * @return
     */
    public fun getChildren(): Collection<Operator>
}
