package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Expr;
import org.partiql.plan.Operator;

import java.util.Collections;
import java.util.List;

/**
 * Match represents a subtree match to be sent to the
 */
public class Match {

    private final Operator[] operators;
    private final List<List<Expr>> children;

    /**
     * Single operator match with zero-or-more inputs.
     *
     * @param operator  matched logical operator.
     * @param children  compiled child operators.
     */
    public Match(@NotNull Operator operator, @NotNull List<Expr> children) {
        this.operators = new Operator[]{operator};
        this.children = Collections.singletonList(children);
    }

    /**
     * Get the i-th operator (pre-order) matched by the pattern.
     *
     * @param i 0-indexed
     * @return Operator
     */
    @NotNull
    public Operator operator(int i) {
        return operators[i];
    }

    /**
     * Get the i-th input to this pattern.
     *
     * @param i 0-indexed
     * @return Expr
     */
    @NotNull
    public List<Expr> children(int i) {
        return children.get(i);
    }
}
