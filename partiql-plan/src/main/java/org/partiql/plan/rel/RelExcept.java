package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * Logical except abstract base class.
 */
public abstract class RelExcept extends RelBase {

    /**
     * @return new {@link RelExcept} instance
     */
    @NotNull
    public static RelExcept create(@NotNull Rel left, @NotNull Rel right, boolean all) {
        return new Impl(left, right, all);
    }

    /**
     * @return left input (operand 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * @return right input (operand 1)
     */
    @NotNull
    public abstract Rel getRight();

    /**
     * @return true if ALL else DISTINCT.
     */
    public abstract boolean isAll();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> operands() {
        Rel c0 = getLeft();
        Rel c1 = getRight();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitExcept(this, ctx);
    }

    /**
     * @return copy with new inputs (non-final).
     */
    @NotNull
    public abstract RelExcept copy(@NotNull Rel left, @NotNull Rel right);

    /**
     * @return copy with new inputs and args (non-final).
     */
    @NotNull
    public abstract RelExcept copy(@NotNull Rel left, @NotNull Rel right, boolean all);

    private static class Impl extends RelExcept {

        private final Rel left;
        private final Rel right;
        private final boolean all;

        private Impl(Rel left, Rel right, boolean all) {
            this.left = left;
            this.right = right;
            this.all = all;
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

        @Override
        public boolean isAll() {
            return all;
        }

        /**
         * @return copy with new inputs (non-final).
         */
        @NotNull
        @Override
        public RelExcept copy(@NotNull Rel left, @NotNull Rel right) {
            return new Impl(left, right, all);
        }

        /**
         * @return copy with new inputs and args (non-final).
         */
        @NotNull
        @Override
        public RelExcept copy(@NotNull Rel left, @NotNull Rel right, boolean all) {
            return new Impl(left, right, all);
        }
    }
}
