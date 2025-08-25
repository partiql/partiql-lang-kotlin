package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.value.Datum;

/**
 * Represents an executable window function.
 * @see org.partiql.plan.WindowFunctionNode
 */
public interface WindowFunction {

    /**
     * Resets the state of the window function. Implementers may hold a reference to {@code partition}.
     * @param partition the partition to reset the window function for
     */
    void reset(@NotNull WindowPartition partition);

    /**
     * Computes the result of the window function for the given environment and ordering group
     * @param env the environment to use for evaluation
     * @param orderingGroupStart the starting row index of the ordering group
     * @param orderingGroupEnd the ending row index of the ordering group
     * @return the result of the window function evaluation
     */
    @NotNull
    Datum eval(@NotNull Environment env, long orderingGroupStart, long orderingGroupEnd);
}
