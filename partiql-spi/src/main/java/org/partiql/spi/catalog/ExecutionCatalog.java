package org.partiql.spi.catalog;

import org.jetbrains.annotations.NotNull;

/**
 * Per-catalog interface for resolving table references at execution time.
 * <p>
 * Database owners implement this to provide table data to the PartiQL VM.
 * The integer IDs correspond to those in the {@link org.partiql.plan.SymbolTable} returned by the planner.
 * <p>
 * Functions and aggregates are assumed thread-safe (stateless invoke, fresh accumulators)
 * and are embedded directly in the plan — only tables need lazy resolution since their data
 * may change between executions.
 * <p>
 * Methods are invoked lazily — the VM only calls {@link #getTable} when the operator tree actually scans the table.
 */
public interface ExecutionCatalog {

    /**
     * Returns the table for the given plan-assigned ID.
     *
     * @param id the table identifier assigned during planning
     * @return the table instance
     */
    @NotNull
    Table getTable(int id);
}
