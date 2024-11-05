package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Expr;
import org.partiql.plan.Operator;

/**
 * Strategy converts a logical operator into a physical operator. The compiler uses the list of operands
 * to determine a subtree match, then invokes `apply` to produce an {@link Expr}.
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
     * @return the physical operation
     */
    @NotNull
    public abstract Expr apply(@NotNull Match match);
}
