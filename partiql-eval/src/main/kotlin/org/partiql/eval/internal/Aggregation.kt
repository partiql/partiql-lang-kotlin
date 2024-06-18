package org.partiql.eval.internal

interface Aggregation {

    public fun getKey(): String

    /**
     * Instantiates a stateful accumulator for this aggregation function.
     *
     * @return
     */
    public fun accumulator(): Accumulator
}
