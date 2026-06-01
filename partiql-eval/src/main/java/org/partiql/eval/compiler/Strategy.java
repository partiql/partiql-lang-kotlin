package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Expr;
import org.partiql.eval.Mode;
import org.partiql.plan.Operator;

import java.util.function.Supplier;

/**
 * Strategy converts a logical operator into a physical operator factory.
 * <p>
 * The compiler uses the pattern to determine a subtree match, then invokes {@link #applyFactory}
 * to produce a supplier of {@link Expr}. The supplier is called on each execution to produce
 * fresh operator instances — safe for concurrent use.
 * <p>
 * For backward compatibility, implementations may override the deprecated {@link #apply} method.
 * New implementations should override {@link #applyFactory} directly.
 */
public abstract class Strategy {

    @NotNull
    private final Pattern pattern;

    /**
     * Create a strategy for a given pattern.
     *
     * @param pattern strategy pattern.
     */
    public Strategy(@NotNull Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Get the pattern associated with this strategy.
     * @return the pattern associated with this strategy
     */
    @NotNull
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Applies the strategy to a logical plan operator and returns the physical operation (expr).
     *
     * @param match holds the matched operators
     * @param mode evaluation mode
     * @param callback for compiling arguments of matched operators
     * @return the physical operation
     * @deprecated Override {@link #applyFactory} instead for thread-safe execution.
     */
    @Deprecated
    @NotNull
    public Expr apply(@NotNull Match match, @NotNull Mode mode, @NotNull Callback callback) {
        throw new UnsupportedOperationException("Override applyFactory() for thread-safe strategies");
    }

    /**
     * Returns an operator factory for the matched plan node.
     * <p>
     * The returned supplier MUST produce independent {@link Expr} instances on each invocation.
     * The supplier itself must be thread-safe (typically stateless or capturing only immutable data).
     * <p>
     * Default implementation wraps the deprecated {@link #apply} for backward compatibility.
     * New implementations should override this method directly.
     *
     * @param match holds the matched operators
     * @param mode evaluation mode
     * @param callback for compiling arguments of matched operators
     * @return a supplier that produces fresh physical operators on each call
     */
    @NotNull
    @SuppressWarnings("deprecation")
    public Supplier<Expr> applyFactory(@NotNull Match match, @NotNull Mode mode, @NotNull Callback callback) {
        final Expr expr = apply(match, mode, callback);
        return () -> expr;
    }

    /**
     * A compilation callback for strategies to compile arguments of matched operators.
     */
    public interface Callback {

        /**
         * Compiles the given logical operator into a physical operator.
         * @param operator the logical operator to compile
         * @return a physical operator (expr) for the logical operator.
         */
        @NotNull
        Expr apply(@NotNull Operator operator);
    }
}
