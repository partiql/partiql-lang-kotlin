package org.partiql.eval.internal

/**
 *
 */
internal fun interface Aggregation {

    /**
     * Instantiates a stateful accumulator for this aggregation function.
     *
     * @return
     */
    fun accumulator(): Accumulator
}
