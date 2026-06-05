package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.internal.vm.StandardVM;
import org.partiql.spi.Context;
import org.partiql.spi.catalog.ExecutionCatalog;
import org.partiql.spi.errors.PRuntimeException;
import org.partiql.spi.value.Datum;

/**
 * Executes a compiled {@link ExecutionPlan} with execution catalogs.
 * <p>
 * The plan holds integer references (catalog ID, table ID) instead of live objects.
 * At execution time, the VM resolves references lazily through the provided {@link ExecutionCatalog} array.
 * <p>
 * Mode (PERMISSIVE/STRICT) is baked into the {@link ExecutionPlan} at compile time.
 * <p>
 * <b>Note:</b> The {@code PartiQLVM} instance itself is not currently thread-safe. Create one instance per thread,
 * or use a single instance with external synchronization. This restriction may be lifted in a future release.
 */
public interface PartiQLVM {

    /**
     * Execute the plan against the given execution catalogs.
     *
     * @param plan     the compiled plan (immutable, cacheable, mode baked in)
     * @param catalogs the execution catalogs indexed by catalog ID from the plan's symbol table
     * @return the query result
     * @throws PRuntimeException if an error is encountered during execution
     */
    @NotNull
    Datum execute(@NotNull ExecutionPlan plan, @NotNull ExecutionCatalog[] catalogs) throws PRuntimeException;

    /**
     * Execute the plan against the given execution catalogs with a context.
     *
     * @param plan     the compiled plan (immutable, cacheable, mode baked in)
     * @param catalogs the execution catalogs indexed by catalog ID from the plan's symbol table
     * @param ctx      the execution context
     * @return the query result
     * @throws PRuntimeException if an error is encountered during execution
     */
    @NotNull
    Datum execute(@NotNull ExecutionPlan plan, @NotNull ExecutionCatalog[] catalogs, @NotNull Context ctx) throws PRuntimeException;

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
