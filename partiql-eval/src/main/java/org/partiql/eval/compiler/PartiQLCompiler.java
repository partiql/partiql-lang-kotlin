package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.ExecutionPlan;
import org.partiql.eval.Mode;
import org.partiql.eval.Statement;
import org.partiql.eval.internal.compiler.StandardCompiler;
import org.partiql.plan.Plan;
import org.partiql.spi.Context;
import org.partiql.spi.catalog.ExecutionCatalog;
import org.partiql.spi.errors.PRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 * PartiQLCompiler is responsible for transforming PartiQL plans to an executable statement (think physical planner).
 */
public interface PartiQLCompiler {

    /**
     * Prepares the given plan into an executable PartiQL statement.
     *
     * @param plan The plan to compile.
     * @param mode The mode to use when compiling the plan.
     * @return The prepared statement.
     * @throws PRuntimeException If an error occurs during compilation.
     * @deprecated Use {@link #compile(Plan)} with {@link org.partiql.eval.PartiQLVM} for thread-safe execution.
     */
    @Deprecated
    @NotNull
    default Statement prepare(@NotNull Plan plan, @NotNull Mode mode) throws PRuntimeException {
        return prepare(plan, mode, Context.standard());
    }

    /**
     * Prepares the given plan into an executable PartiQL statement.
     *
     * @param plan The plan to compile.
     * @param mode The mode to use when compiling the plan.
     * @param ctx The context to use when compiling the plan.
     * @throws PRuntimeException If an error occurs during compilation. The error might have been emitted by the {@code ctx}'s {@link Context#getErrorListener()}.
     * @return The prepared statement.
     * @deprecated Use {@link #compile(Plan)} with {@link org.partiql.eval.PartiQLVM} for thread-safe execution.
     */
    @Deprecated
    @NotNull
    public Statement prepare(@NotNull Plan plan, @NotNull Mode mode, @NotNull Context ctx) throws PRuntimeException;

    /**
     * Compiles a ref-based plan into a thread-safe, cacheable {@link ExecutionPlan}.
     * <p>
     * The plan must use reference nodes (produced by a planner with {@code useRefs()} enabled).
     * Plans containing embedded objects (e.g., from the deprecated non-ref path) will be rejected.
     *
     * @param plan The ref-based plan to compile.
     * @return An opaque, thread-safe execution plan.
     * @throws PRuntimeException If the plan contains embedded objects (non-ref nodes).
     */
    @NotNull
    ExecutionPlan compile(@NotNull Plan plan) throws PRuntimeException;

    /**
     * Prepares a ref-based plan into a thread-safe {@link Statement} that resolves references
     * from the provided {@link ExecutionCatalog} array on each {@link Statement#execute()} call.
     * <p>
     * Each call to {@link Statement#execute()} builds a fresh operator tree — safe for concurrent use.
     * Use this when you want the convenience of {@link Statement} with thread-safe ref-based plans.
     *
     * @param plan     The ref-based plan (produced by a planner with {@code useRefs()} enabled).
     * @param mode     The execution mode (permissive or strict).
     * @param catalogs The execution catalogs indexed by catalog ID from the plan's symbol table.
     * @return A thread-safe statement.
     * @throws PRuntimeException If the plan contains embedded objects or compilation fails.
     */
    @NotNull
    default Statement prepare(@NotNull Plan plan, @NotNull Mode mode, @NotNull ExecutionCatalog[] catalogs) throws PRuntimeException {
        return prepare(plan, mode, catalogs, Context.standard());
    }

    /**
     * Prepares a ref-based plan into a thread-safe {@link Statement} that resolves references
     * from the provided {@link ExecutionCatalog} array on each {@link Statement#execute()} call.
     *
     * @param plan     The ref-based plan (produced by a planner with {@code useRefs()} enabled).
     * @param mode     The execution mode (permissive or strict).
     * @param catalogs The execution catalogs indexed by catalog ID from the plan's symbol table.
     * @param ctx      The context.
     * @return A thread-safe statement.
     * @throws PRuntimeException If the plan contains embedded objects or compilation fails.
     */
    @NotNull
    Statement prepare(@NotNull Plan plan, @NotNull Mode mode, @NotNull ExecutionCatalog[] catalogs, @NotNull Context ctx) throws PRuntimeException;

    /**
     * Returns a new {@link Builder}.
     * @return a new {@link Builder}.
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a new {@link PartiQLCompiler}.
     */
    public static PartiQLCompiler standard() {
        return new StandardCompiler();
    }

    /**
     * Builder class for the {@link PartiQLCompiler} interface.
     */
    public static class Builder {

        //
        private final List<Strategy> strategies = new ArrayList<>();

        private Builder() {
            // empty
        }

        /**
         * Adds a strategy to the compiler.
         *
         * @param strategy The strategy to add.
         * @return this.
         */
        public Builder addStrategy(Strategy strategy) {
            strategies.add(strategy);
            return this;
        }

        /**
         * @return A new [PartiQLCompiler].
         */
        public PartiQLCompiler build() {
            return new StandardCompiler(strategies);
        }
    }
}
