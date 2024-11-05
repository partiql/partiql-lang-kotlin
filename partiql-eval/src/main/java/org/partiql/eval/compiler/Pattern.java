package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.Operator;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Pattern defines a tree pattern like a Calcite RelOptRuleOperand.
 */
public class Pattern {

    /**
     *  T – match if class T, check children.
     *  ? - match any node, check children.
     *  * - match all nodes in the subtree.
     */
    private final Kind kind;

    /**
     * IF strict THEN match children exactly ELSE ignore unspecified children.
     */
    private final boolean strict;

    @Nullable
    private final Class<? extends Operator> clazz;

    @Nullable
    private final Predicate<Operator> predicate;

    @NotNull
    private final List<Pattern> children;

    private <T extends Operator> Pattern(
            Kind kind,
            boolean strict,
            @Nullable Class<T> clazz,
            @Nullable Predicate<? super T> predicate,
            @NotNull List<Pattern> children
    ) {
        this.kind = kind;
        this.strict = strict;
        this.clazz = clazz;
        this.predicate = (Predicate<Operator>) predicate;
        this.children = children;
    }

    public boolean matches(Operator operator) {

        if (kind == Kind.ANY) {
            //
            return true;
        }

        if (!clazz.isInstance(operator)) {
            return false;
        }

        return predicate == null || predicate.test(operator);
    }

    @NotNull
    public static <T extends Operator> Pattern simple(Class<T> clazz) {
        return new Pattern(Kind.TYPE, false, clazz, null, Collections.emptyList());
    }

    @NotNull
    public static <T extends Operator> Builder<T> match(Class<T> clazz) {
        return new Builder<>(Kind.TYPE, clazz);
    }

    @NotNull
    public static Builder<?> any() {
        return new Builder<>(Kind.ANY);
    }

    @NotNull
    public static Builder<?> all() {
        return new Builder<>(Kind.ALL);
    }

    /**
     * Builder for a pattern tree; like Calcite RelRule.OperandBuilder.
     * <br>
     * Example of a filter we want to push through a join.
     * <pre>
     *
     *       filter
     *          \
     *          join
     *         /    \
     *        ?      ?
     *
     *        where(filter exclusive to join.child(0) OR filter exclusive to join.child(1))
     *
     * </pre>
     * <code>
     * Pattern.match(RelFilter::class)
     *        .predicate(::filterCanMoveInsideJoin)
     *        .child(Pattern.match(RelJoin::class))
     *        .build();
     * </code>
     * <br>
     * Some tips:
     *   - Matching all children is the default, use .none() if you want to explicitly not match children.
     *   - Inputs are added in order, but you can use .child(n, pattern) if you want to set child n explicitly.
     *   - You can set multiple children in order, with .children(vararg pattern)
     *   - The .child() methods always override previous calls.
     */
    public static class Builder<T extends Operator> {

        private final Kind kind;
        private final Class<T> clazz;
        private Predicate<? super T> predicate;
        private List<Pattern> children = Collections.emptyList();

        private Builder(Kind kind) {
            this.kind = kind;
            this.clazz = null;
        }

        private Builder(Kind kind, Class<T> clazz) {
            this.kind = kind;
            this.clazz = clazz;
        }

        /**
         * Set the pattern predicate.
         *
         * @param predicate Predicate
         * @return Builder
         */
        public Builder<T> predicate(Predicate<T> predicate) {
            this.predicate = predicate;
            return this;
        }

        /**
         * Set current child to the pattern.
         *
         * @param pattern Pattern
         * @return Builder
         */
        public Builder<T> child(Pattern pattern) {
            this.children.add(pattern);
            return this;
        }

        /**
         * Add a simple child match.
         *
         * @param clazz child pattern type
         * @return Builder
         * @param <S> child class
         */
        public <S extends Operator> Builder<T> child(Class<S> clazz) {
            Pattern pattern = Pattern.simple(clazz);
            return child(pattern);
        }

        /**
         * Set child at `index` to the pattern.
         *
         * @param index int
         * @param pattern Pattern
         * @return Builder
         */
        public Builder<T> child(int index, Pattern pattern) {
            this.children.set(index, pattern);
            return this;
        }

        /**
         * Set child at `index` to a simple child match.
         *
         * @param clazz child pattern type
         * @return Builder
         * @param <S> child class
         */
        public <S extends Operator> Builder<T> child(int index, Class<S> clazz) {
            Pattern pattern = Pattern.simple(clazz);
            return child(index, pattern);
        }

        /**
         * Set children to the given list.
         *
         * @param patterns Pattern...
         * @return Builder
         */
        public Builder<T> children(Pattern... patterns) {
            this.children.clear();
            Collections.addAll(this.children, patterns);
            return this;
        }

        public Pattern build() {
            return new Pattern(kind, true, clazz, predicate, children);
        }
    }

    /**
     * This pattern's matching behavior.
     * <pre>
     *
     *   T – match if class T, check children.
     *   ? - match any node, check children.
     *   * - match all nodes in the subtree.
     *
     * </pre>
     */
    private enum Kind {
        TYPE, // T
        ANY,
        ALL;

        /**
         * Pattern debug string.
         */
        @Override
        public String toString() {
            switch (this) {
                case TYPE:
                    return "T";
                case ANY:
                    return "?";
                case ALL:
                    return "*";
                default:
                    throw new IllegalArgumentException("unreachable");
            }
        }
    }
}
