package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Plan;

/**
 * A [PartiQLCompiler] is responsible for produces physical plans from logical plans.
 */
public interface PartiQLCompiler {

    /**
     * Compiles the given plan into an executable PartiQL statement.
     *
     * @param plan The plan to compile.
     * @return The prepared statement.
     */
    @NotNull
    public Statement prepare(@NotNull Plan plan);

    /**
     * @return A new [PartiQLCompilerBuilder].
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A new [PartiQLCompiler] with the default (strict) compilation mode.
     */
    @NotNull
    public static PartiQLCompiler standard() {
        return standard(Mode.STRICT());
    }

    /**
     * @param mode The compilation mode.
     * @return A new [PartiQLCompiler] with the given compilation mode.
     */
    @NotNull
    public static PartiQLCompiler standard(Mode mode) {
        return new StandardCompiler(mode);
    }

    /**
     * Builder class for the [PartiQLCompiler] interface.
     */
    public static class Builder {

        // builder state
        private Mode mode;

        private Builder() {
            // empty
        }

        /**
         * @param mode The mode.
         * @return This builder.
         */
        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * @return A new [PartiQLCompiler].
         */
        public PartiQLCompiler build() {
            return new StandardCompiler(mode);
        }
    }
}
