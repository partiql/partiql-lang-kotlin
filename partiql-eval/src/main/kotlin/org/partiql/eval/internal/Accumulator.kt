package org.partiql.eval.internal

import org.partiql.eval.value.Datum

/**
 * Aggregation function state.
 *
 * TODO consider a `done()` method for short-circuiting.
 */
interface Accumulator {

    /**
     * Apply args to the accumulator.
     *
     * @param args
     * @return
     */
    fun next(args: Array<Datum>)

    /**
     * Return the accumulator value.
     *
     * @return
     */
    fun value(): Datum
}
