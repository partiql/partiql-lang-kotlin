package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.JoinType;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical join abstract base class.
 */
public abstract class RelJoin extends RelBase {

    /**
     * @return new {@link RelJoin} instance
     */
    @NotNull
    public static RelJoin create(@NotNull Rel left, @NotNull Rel right, @Nullable Rex condition, @NotNull JoinType joinType) {
        return new Impl(left, right, condition, joinType);
    }

    /**
     * @return left input (child 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * @return right input (child 1)
     */
    @NotNull
    public abstract Rel getRight();

    /**
     * @return the join condition (child 2), or null if there is no condition.
     */
    @Nullable
    public abstract Rex getCondition();

    /**
     * @return JoinType
     */
    @NotNull
    public abstract JoinType getJoinType();

    @Override
    protected RelType type() {
        throw new UnsupportedOperationException("compute join type");
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getLeft();
        Rel c1 = getRight();
        Rex c2 = getCondition(); // can be null!
        return List.of(c0, c1, c2);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitJoin(this, ctx);
    }

    private static class Impl extends RelJoin {

        private final Rel left;
        private final Rel right;
        private final Rex condition;
        private final JoinType joinType;

        public Impl(Rel left, Rel right, Rex condition, JoinType joinType) {
            this.left = left;
            this.right = right;
            this.condition = condition;
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

        @Nullable
        @Override
        public Rex getCondition() {
            return condition;
        }

        @NotNull
        @Override
        public JoinType getJoinType() {
            return joinType;
        }
    }
}
