package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Mode;
import org.partiql.eval.internal.compiler.StandardCompiler;
import org.partiql.eval.Statement;
import org.partiql.plan.Plan;

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
     * @return The prepared statement.
     */
    @NotNull
    public Statement prepare(@NotNull Plan plan, @NotNull Mode mode);

    /**
     * @return A new [PartiQLCompilerBuilder].
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A new [PartiQLCompiler].
     */
    public static PartiQLCompiler standard() {
        return new StandardCompiler();
    }

    /**
     * Builder class for the [PartiQLCompiler] interface.
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
         * @return This builder.
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
