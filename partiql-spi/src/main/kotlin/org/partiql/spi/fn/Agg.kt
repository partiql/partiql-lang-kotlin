package org.partiql.spi.fn

import org.partiql.spi.value.Datum

/**
 * Represents an SQL table-value expression call.
 */
public interface Agg {

    /**
     * Aggregation function signature.
     */
    public val signature: AggSignature

    /**
     * Instantiates a stateful accumulator for this aggregation function.
     *
     * @return
     */
    public fun accumulator(): Accumulator

    /**
     * Aggregation function state.
     */
    public interface Accumulator {

        /**
         * Apply args to the accumulator.
         *
         * @param args
         * @return
         */
        public fun next(args: Array<Datum>)

        /**
         * Return the accumulator value.
         *
         * @return
         */
        public fun value(): Datum
    }
}
