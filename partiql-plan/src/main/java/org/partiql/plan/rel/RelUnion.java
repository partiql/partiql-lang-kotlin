package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical union abstract base class.
 */
public abstract class RelUnion extends RelBase {

    /**
     * @return new {@link RelUnion} instance
     */
    @NotNull
    public static RelUnion create(@NotNull Rel left, @NotNull Rel right, boolean all) {
        return new Impl(left, right, all);
    }

    /**
     * @return true if ALL else DISTINCT.
     */
    public abstract boolean isAll();

    /**
     * @return left rel (child 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * @return right rel (child 1)
     */
    @NotNull
    public abstract Rel getRight();

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
        return visitor.visitUnion(this, ctx);
    }

    private static class Impl extends RelUnion {

        private final Rel left;
        private final Rel right;
        private final boolean all;

        public Impl(Rel left, Rel right, boolean all) {
            this.left = left;
            this.right = right;
            this.all = all;
        }

        @Override
        public boolean isAll() {
            return all;
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
    }
}
