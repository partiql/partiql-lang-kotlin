package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Plan;
import org.partiql.spi.value.Datum;

/**
 * TODO JAVADOC
 */
public interface PartiQLEvaluator {

    /**
     * @param plan The plan to evaluate.
     * @return The result of evaluating the plan.
     */
    @NotNull
    public Datum eval(@NotNull Plan plan);

    /**
     * @return A new [PartiQLCompilerBuilder].
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A new [PartiQLEvaluator] with the default (strict) evaluation mode.
     */
    @NotNull
    public static PartiQLEvaluator standard() {
        return standard(Mode.STRICT());
    }

    /**
     * @param mode The evaluation mode.
     * @return A new [PartiQLEvaluator] with the given evaluation mode.
     */
    @NotNull
    public static PartiQLEvaluator standard(Mode mode) {
        return new StandardEvaluator(PartiQLCompiler.standard(mode));
    }

    /**
     * Builder class for the [PartiQLCompiler] interface.
     */
    public static class Builder {

        // builder state
        private PartiQLCompiler compiler = PartiQLCompiler.standard();

        private Builder() {
            // empty
        }

        public Builder compiler(PartiQLCompiler compiler) {
            this.compiler = compiler;
            return this;
        }

        /**
         * @return A new [PartiQLCompiler].
         */
        public PartiQLEvaluator build() {
            return new StandardEvaluator(compiler);
        }
    }
}
