package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.Operator;

import java.util.function.Predicate;

/**
 * Pattern defines a tree pattern.
 */
public class Pattern {

    @NotNull
    private final Class<? extends Operator> clazz;

    @Nullable
    private final Predicate<Operator> predicate;

    /**
     * The only public method to create a pattern for now is this single-node match.
     *
     * @param clazz Operator class.
     */
    public Pattern(@NotNull Class<? extends Operator> clazz) {
        this.clazz = clazz;
        this.predicate = null;
    }

    /**
     * Internal constructor for simple patterns.
     *
     * @param clazz     root type.
     * @param predicate optional predicate.
     */
    protected Pattern(@NotNull Class<? extends Operator> clazz, @Nullable Predicate<Operator> predicate) {
        this.clazz = clazz;
        this.predicate = predicate;
    }

    public boolean matches(Operator operator) {
        if (!clazz.isInstance(operator)) {
            return false;
        }
        return predicate == null || predicate.test(operator);
    }

}
