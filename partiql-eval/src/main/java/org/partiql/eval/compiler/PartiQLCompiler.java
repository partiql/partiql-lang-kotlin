package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Mode;
import org.partiql.eval.Statement;
import org.partiql.eval.internal.compiler.StandardCompiler;
import org.partiql.plan.Plan;
import org.partiql.spi.Context;
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
     */
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
     */
    @NotNull
    public Statement prepare(@NotNull Plan plan, @NotNull Mode mode, @NotNull Context ctx) throws PRuntimeException;

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
