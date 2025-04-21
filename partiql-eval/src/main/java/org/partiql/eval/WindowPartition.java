package org.partiql.eval;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a partition of rows within a window.
 */
public interface WindowPartition {

    /**
     * Returns the number of rows in this partition.
     * @return the number of rows in this partition.
     */
    long size();

    /**
     * Retrieves the row at the given index.
     * @param index the index of the row to retrieve. Partitions are 0-indexed.
     * @throws RuntimeException if index is out of bounds
     * @return the row at the given index
     */
    @NotNull
    Row get(long index) throws RuntimeException;
}
