package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Expr;
import org.partiql.plan.Operator;

import java.util.ArrayList;
import java.util.List;

/**
 * Match represents a subtree match to be sent to the
 */
public class Match {

    private final Operator[] matched;
    private final List<List<Expr>> children;

    /**
     * Single operator match, no inputs.
     */
    public Match(Operator operator) {
        this.matched = new Operator[]{operator};
        this.children = new ArrayList[]{};
    }

    /**
     * Operator tree match with zero-or-more inputs.
     *
     * @param matched  matched logical operands.
     * @param children    compile physical inputs.
     */
    public Match(@NotNull Operator[] matched, @NotNull Expr[] children) {
        this.matched = matched;
        this.children = children;
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
        return children[i];
    }
}
