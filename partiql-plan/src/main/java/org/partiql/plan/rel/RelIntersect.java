package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * Logical intersect abstract base class.
 */
public abstract class RelIntersect extends RelBase {

    /**
     * @return new {@link RelIntersect} instance
     */
    @NotNull
    public static RelIntersect create(@NotNull Rel left, @NotNull Rel right, boolean all) {
        return new Impl(left, right, all);
    }

    /**
     * @return left rel (operand 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * @return right rel (operand 1)
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
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getLeft());
        Operand c1 = Operand.single(getRight());
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitIntersect(this, ctx);
    }

    @NotNull
    public abstract RelIntersect copy(@NotNull Rel left, @NotNull Rel right);

    @NotNull
    public abstract RelIntersect copy(@NotNull Rel left, @NotNull Rel right, boolean all);

    private static class Impl extends RelIntersect {

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

        @NotNull
        @Override
        public RelIntersect copy(@NotNull Rel left, @NotNull Rel right) {
            return new Impl(left, right, all);
        }

        @NotNull
        @Override
        public RelIntersect copy(@NotNull Rel left, @NotNull Rel right, boolean all) {
            return new Impl(left, right, all);
        }
    }
}
