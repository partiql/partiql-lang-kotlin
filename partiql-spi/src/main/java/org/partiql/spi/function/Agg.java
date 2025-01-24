package org.partiql.spi.function;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an aggregate function (e.g. SUM, AVG, etc.) and its implementation.
 * @see AggOverload
 */
public abstract class Agg {

    /**
     * Returns an accumulator for the aggregate function.
     * @return an accumulator for the aggregate function.
     */
    @NotNull
    public abstract Accumulator getAccumulator();

    /**
     * Returns the signature of the aggregate function.
     * @return the signature of the aggregate function.
     */
    @NotNull
    public abstract RoutineSignature getSignature();
}
