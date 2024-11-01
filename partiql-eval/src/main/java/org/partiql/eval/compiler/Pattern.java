package org.partiql.eval.compiler;

import org.jetbrains.annotations.NotNull;
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
     * IF open THEN match any additional operators ELSE reject any additional operators.
     */
    private final boolean open;

    private final Class<? extends Operator> clazz;
    private final Predicate<Operator> predicate;
    private final List<Pattern> children;

    private Pattern(
            Kind kind,
            boolean open,
            Class<? extends Operator> clazz,
            Predicate<Operator> predicate,
            List<Pattern> children
    ) {
        this.kind = kind;
        this.open = open;
        this.clazz = clazz;
        this.predicate = predicate;
        this.children = children;
    }

    public boolean matches(Operator operator) {
        if (!clazz.isInstance(operator)) {
            return false;
        }
        return predicate.test(operator);
    }

    @NotNull
    public static <T extends Operator> Builder root(Class<T> clazz) {
        return new Builder(Kind.TYPE, clazz);
    }

    // TODO add the any builder
    // TODO add the all builder

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
     * </code>
     * Pattern.root(RelFilter::class)
     *        .predicate(filter -> filterCanMoveInsideJoin(filter))
     *        .child(Pattern.root(RelJoin::class))
     *        .build();
     * </code>
     * <br>
     * Some tips:
     *   - Matching all children is the default, use .none() if you want to explicitly not match children.
     *   - Children are added in order, but you can use .child(n, pattern) if you want to set child n.
     *   - You can set multiple children, in order, with .children(vararg pattern)
     *   - The .child() methods always override previous calls.
     *   - Calling .
     */
    public static class Builder<T extends Operator> {

        private final Kind kind;
        private final Class<T> clazz;
        private Predicate<? extends Operator> predicate;
        private List<Pattern> children = Collections.emptyList();

        private Builder(Kind kind, Class<T> clazz) {
            this.kind = kind;
            this.clazz = clazz;
        }

        public Builder<T> predicate(Predicate<? extends Operator> predicate) {
            this.predicate = predicate;
            return this;
        }
    }

    /**
     * This pattern's matching behavior.
     * <pre>
     *  T – match if class T, check children.
     *  ? - match any node, check children.
     *  * - match all nodes in the subtree.
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
