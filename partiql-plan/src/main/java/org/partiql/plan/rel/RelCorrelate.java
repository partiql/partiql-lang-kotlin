package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.JoinType;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical nested-loop joins (correlated subqueries, lateral joins, and cross joins) abstract base class.
 * <pre>
 *     l, r <=> l CROSS JOIN r <=> l JOIN r ON TRUE
 * </pre>
 */
public abstract class RelCorrelate extends RelBase {

    /**
     * @return new {@link RelCorrelate} instance
     */
    @NotNull
    public static RelCorrelate create(@NotNull Rel left, @NotNull Rel right, @NotNull JoinType joinType) {
        return new Impl(left, right, joinType);
    }

    /**
     * @return the left input (child 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * @return the right input (child 1)
     */
    @NotNull
    public abstract Rel getRight();

    @NotNull
    public abstract JoinType getJoinType();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getLeft();
        Rel c1 = getRight();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitCorrelate(this, ctx);
    }

    private static class Impl extends RelCorrelate {

        private final Rel left;
        private final Rel right;
        private final JoinType joinType;

        public Impl(Rel left, Rel right, JoinType joinType) {
            this.left = left;
            this.right = right;
            this.joinType = joinType;
        }

        @NotNull
        @Override
        public Rel getLeft() {
            return left;
        }

        @NotNull
        @Override
        public Rel getRight() {
            return right;
        }

        @NotNull
        @Override
        public JoinType getJoinType() {
            return joinType;
        }
    }
}
