package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.value.Datum;

/**
 * Represents the accumulator for an aggregation function.
 * @see AggProvider
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
     * @return the computed final value of the accumulator.
     */
    @NotNull
    public Datum value();
}
