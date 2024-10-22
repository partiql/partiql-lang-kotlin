package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Expr;
import org.partiql.plan.Operator;

import java.util.Collections;
import java.util.function.Predicate;

/**
 * Strategy converts a logical operator into a physical operator. The compiler uses the list of operands
 * to determine a subtree match, then invokes `apply` to
 */
public abstract class Strategy {

    @NotNull
    public final Pattern pattern;

    protected Strategy(@NotNull Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Applies the strategy to a logical plan operator and returns the physical operation (expr).
     *
     * @param match holds the matched operators
     * @return the physical operation
     */
    @NotNull
    public abstract Expr apply(@NotNull Match match);

    // -- PATTERN CONSTRUCTORS

    /**
     * Create an operand that matches the given class.
     */
    @NotNull
    public static Pattern pattern(@NotNull Class<? extends Operator> clazz) {
        return new Pattern(clazz);
    }

    /**
     * Create an operand that matches the given class and predicate.
     */
    @NotNull
    public static Pattern pattern(@NotNull Class<? extends Operator> clazz, @NotNull Predicate<Operator> predicate) {
        return new Pattern(clazz, predicate, Collections.emptyList());
    }
}
