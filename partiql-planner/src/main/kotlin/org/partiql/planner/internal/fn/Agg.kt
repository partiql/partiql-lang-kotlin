package org.partiql.planner.internal.fn

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Represents an SQL table-value expression call.
 */
@FnExperimental
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
        @OptIn(PartiQLValueExperimental::class)
        public fun next(args: Array<PartiQLValue>)

        /**
         * Return the accumulator value.
         *
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        public fun value(): PartiQLValue
    }
}
