package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.value.Datum;

/**
 * Represents the accumulator for an aggregation function.
 * @see AggOverload
 * @see Agg
 */
public interface Accumulator {

    /**
     * Receives the next set of arguments, and updates the accumulator's state.
     * @param args the next set of arguments.
     */
    public void next(Datum[] args);

    /**
     * Computes the final value of the accumulator.
     * <p>
     * Implementations must not modify the accumulator's state, as this may be called more than once, interleaved with
     * calls to {@link #next(Datum[])}. For example, when an aggregate is used as a window function
     * (e.g. {@code SUM(x) OVER (ORDER BY y)}), the running value is computed after each group of rows is received.
     * </p>
     * @return the computed final value of the accumulator.
     */
    @NotNull
    public Datum value();
}
