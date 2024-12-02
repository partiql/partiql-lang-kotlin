package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical scan abstract base class.
 */
public abstract class RelScan extends RelBase {

    /**
     * @return new {@link RelScan} instance
     */
    @NotNull
    public static RelScan create(@NotNull Rex rex) {
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
        return visitor.visitScan(this, ctx);
    }

    @NotNull
    public abstract RelScan copy(@NotNull Rex rex);

    private static class Impl extends RelScan {

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
        public RelScan copy(@NotNull Rex rex) {
            return new Impl(rex);
        }
    }
}
