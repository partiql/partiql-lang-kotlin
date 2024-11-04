package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Expr;
import org.partiql.plan.Operator;

/**
 * Match represents a subtree match to be sent to the
 */
public class Match {

    private final Operator[] matched;
    private final Expr[] inputs;

    /**
     * Single operator match, no inputs.
     */
    public Match(Operator operator) {
        this.matched = new Operator[]{operator};
        this.inputs = new Expr[]{};
    }

    /**
     * Operator tree match with zero-or-more inputs.
     *
     * @param matched  matched logical operands.
     * @param inputs    compile physical inputs.
     */
    public Match(@NotNull Operator[] matched, @NotNull Expr[] inputs) {
        this.matched = matched;
        this.inputs = inputs;
    }

    /**
     * Get the i-th operator (pre-order) matched by the pattern.
     *
     * @param i 0-indexed
     * @return Operator
     */
    @NotNull
    public Operator get(int i) {
        return matched[i];
    }

    /**
     * Get the i-th input to this pattern.
     *
     * @param i 0-indexed
     * @return Expr
     */
    @NotNull
    public Expr input(int i) {
        return inputs[i];
    }
}
