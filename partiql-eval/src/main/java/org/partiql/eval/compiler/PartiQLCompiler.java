package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Mode;
import org.partiql.eval.internal.compiler.StandardCompiler;
import org.partiql.eval.Statement;
import org.partiql.plan.Plan;
import org.partiql.spi.Context;

/**
 * TODO JAVADOC
 */
public interface PartiQLCompiler {

    /**
     * Prepares the given plan into an executable PartiQL statement.
     *
     * @param plan The plan to compile.
     * @return The prepared statement.
     */
    @NotNull
    default Statement prepare(@NotNull Plan plan, @NotNull Mode mode) {
        return prepare(plan, mode, Context.standard());
    }

    @NotNull
    public Statement prepare(@NotNull Plan plan, @NotNull Mode mode, @NotNull Context ctx);


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

        private Builder() {
            // empty
        }

        /**
         * @return A new [PartiQLCompiler].
         */
        public PartiQLCompiler build() {
            return new StandardCompiler();
        }
    }
}
