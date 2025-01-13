package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.JoinType;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical theta-join abstract base class.
 */
public abstract class RelJoin extends RelBase {

    /**
     * Creates a new {@link RelJoin} instance.
     *
     * @param left left input (operand 0)
     * @param right right input (operand 1)
     * @param condition join condition
     * @param joinType join type
     * @return new {@link RelJoin} instance
     */
    @NotNull
    public static RelJoin create(@NotNull Rel left, @NotNull Rel right, @NotNull Rex condition, @NotNull JoinType joinType) {
        return new Impl(left, right, condition, joinType);
    }

    /**
     * Gets the left input.
     * @return left input (operand 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * Gets the right input.
     * @return right input (operand 1)
     */
    @NotNull
    public abstract Rel getRight();

    /**
     * Gets the join type.
     * @return JoinType
     */
    @NotNull
    public abstract JoinType getJoinType();

    /**
     * Gets the join condition.
     * @return the join condition.
     */
    @NotNull
    public abstract Rex getCondition();

    @NotNull
    @Override
    protected RelType type() {
        throw new UnsupportedOperationException("compute join type");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getLeft());
        Operand c1 = Operand.single(getRight());
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitJoin(this, ctx);
    }

    @NotNull
    public abstract RelJoin copy(@NotNull Rel left, @NotNull Rel right);

    @NotNull
    public abstract RelJoin copy(@NotNull Rel left, @NotNull Rel right, @NotNull Rex condition, @NotNull JoinType joinType);

    private static class Impl extends RelJoin {

        private final Rel left;
        private final Rel right;
        private final Rex condition;
        private final JoinType joinType;

        private Impl(Rel left, Rel right, Rex condition, JoinType joinType) {
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

        @NotNull
        @Override
        public Rex getCondition() {
            return condition;
        }

        @NotNull
        @Override
        public JoinType getJoinType() {
            return joinType;
        }

        @NotNull
        @Override
        public RelJoin copy(@NotNull Rel left, @NotNull Rel right) {
            return new Impl(left, right, condition, joinType);
        }

        @NotNull
        @Override
        public RelJoin copy(@NotNull Rel left, @NotNull Rel right, @NotNull Rex condition, @NotNull JoinType joinType) {
            return new Impl(left, right, condition, joinType);
        }
    }
}
