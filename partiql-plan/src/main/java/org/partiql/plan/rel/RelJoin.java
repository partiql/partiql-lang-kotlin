package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.JoinType;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical theta-join abstract base class.
 */
public abstract class RelJoin extends RelBase {

    /**
     * @return new {@link RelJoin} instance
     */
    @NotNull
    public static RelJoin create(@NotNull Rel left, @NotNull Rel right, @NotNull JoinType joinType, @NotNull Rex condition) {
        return new Impl(left, right, joinType, condition);
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
     * @return JoinType
     */
    @NotNull
    public abstract JoinType getJoinType();

    /**
     * @return the join condition (child 2), or null if there is no condition.
     */
    @NotNull
    public abstract Rex getCondition();

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
        private final JoinType joinType;
        private final Rex condition;

        public Impl(Rel left, Rel right, JoinType joinType, Rex condition) {
            this.left = left;
            this.right = right;
            this.joinType = joinType;
            this.condition = condition;
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

        @NotNull
        @Override
        public Rex getCondition() {
            return condition;
        }
    }
}
