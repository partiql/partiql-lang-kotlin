package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical unpivot abstract base class.
 */
public abstract class RelUnpivot extends RelBase {

    /**
     * @return new {@link RelUnpivot} instance
     */
    @NotNull
    public static RelUnpivot create(@NotNull Rex rex) {
        return new Impl(rex);
    }

    /**
     * @return input rex (operand 0)
     */
    @NotNull
    public abstract Rex getRex();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> operands() {
        Rex c0 = getRex();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitUnpivot(this, ctx);
    }

    @NotNull
    public abstract RelUnpivot copy(@NotNull Rex rex);

    private static class Impl extends RelUnpivot {

        private final Rex rex;

        private Impl(Rex rex) {
            this.rex = rex;
        }

        @NotNull
        @Override
        public Rex getRex() {
            return rex;
        }

        @NotNull
        @Override
        public RelUnpivot copy(@NotNull Rex rex) {
            return new Impl(rex);
        }
    }
}
