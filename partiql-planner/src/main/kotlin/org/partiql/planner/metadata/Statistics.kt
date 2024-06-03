package org.partiql.planner.metadata

/**
 * A [Statistics] object is used by the query planner for table statistics and metadata such as available keys and indexes.
 *
 * IMPORTANT: This is a placeholder.
 */
public interface Statistics {

    /**
     * A [Statistics] object when there's an absence of any table information.
     */
    public object None : Statistics
}
