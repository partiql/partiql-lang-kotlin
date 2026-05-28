package org.partiql.spi.catalog;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.function.Agg;
import org.partiql.spi.function.Fn;
import org.partiql.spi.function.FnOverload;

/**
 * Per-catalog interface for resolving plan references at execution time.
 * <p>
 * Database owners implement this to provide data and functions to the PartiQL VM.
 * The integer IDs correspond to those in the {@link org.partiql.plan.SymbolTable} returned by the planner.
 * <p>
 * Methods are invoked lazily — the VM only calls them when the operator tree actually needs the object during execution.
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

    /**
     * Returns the scalar function for the given plan-assigned ID.
     * Used for statically-resolved function calls.
     *
     * @param id the function identifier assigned during planning
     * @return the function instance
     */
    @NotNull
    Fn getFn(int id);

    /**
     * Returns the function overload for the given plan-assigned ID.
     * Used for dynamically-dispatched function calls where runtime type resolution is needed.
     *
     * @param id the function identifier assigned during planning
     * @return the function overload
     */
    @NotNull
    FnOverload getFnOverload(int id);

    /**
     * Returns the aggregate function for the given plan-assigned ID.
     *
     * @param id the aggregate identifier assigned during planning
     * @return the aggregate instance
     */
    @NotNull
    Agg getAgg(int id);
}
