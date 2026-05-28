package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.internal.vm.StandardVM;
import org.partiql.plan.Plan;
import org.partiql.spi.Context;
import org.partiql.spi.catalog.ExecutionCatalog;
import org.partiql.spi.errors.PRuntimeException;
import org.partiql.spi.value.Datum;

/**
 * Executes a cached, thread-safe plan with execution catalogs.
 * <p>
 * The plan holds integer references (catalog ID, object ID) instead of live objects.
 * At execution time, the VM resolves references lazily through the provided {@link ExecutionCatalog} array.
 * <p>
 * Thread-safe: multiple threads can call {@code execute()} concurrently with the same plan and different catalogs.
 * Each invocation builds a fresh operator tree with no shared mutable state.
 */
public interface PartiQLVM {

    /**
     * Execute the plan against the given execution catalogs.
     *
     * @param plan     the compiled plan (immutable, cacheable)
     * @param mode     the execution mode (permissive or strict)
     * @param catalogs the execution catalogs indexed by catalog ID from the plan's symbol table
     * @return the query result
     * @throws PRuntimeException if an error is encountered during execution
     */
    @NotNull
    Datum execute(@NotNull Plan plan, @NotNull Mode mode, @NotNull ExecutionCatalog[] catalogs) throws PRuntimeException;

    /**
     * Execute the plan against the given execution catalogs with a context.
     *
     * @param plan     the compiled plan (immutable, cacheable)
     * @param mode     the execution mode (permissive or strict)
     * @param catalogs the execution catalogs indexed by catalog ID from the plan's symbol table
     * @param ctx      the execution context
     * @return the query result
     * @throws PRuntimeException if an error is encountered during execution
     */
    @NotNull
    Datum execute(@NotNull Plan plan, @NotNull Mode mode, @NotNull ExecutionCatalog[] catalogs, @NotNull Context ctx) throws PRuntimeException;

    /**
     * Returns a standard PartiQLVM instance.
     *
     * @return a new standard VM
     */
    @NotNull
    static PartiQLVM standard() {
        return new StandardVM();
    }
}
