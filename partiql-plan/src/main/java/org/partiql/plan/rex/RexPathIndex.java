package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical path by index expression abstract base class.
 */
public abstract class RexPathIndex extends RexBase {

    /**
     * @return new RexPathIndex instance
     */
    @NotNull
    public static RexPathIndex create(@NotNull Rex operand, @NotNull Rex index) {
        return new Impl(operand, index);
    }

    /**
     * @return operand rex (child 0)
     */
    @NotNull
    public abstract Rex getOperand();

    /**
     * @return index rex (child 1)
     */
    public abstract Rex getIndex();

    @Override
    @NotNull
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected final List<Operator> children() {
        Rex c0 = getOperand();
        Rex c1 = getIndex();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitPathIndex(this, ctx);
    }

    private static class Impl extends RexPathIndex {

        private final Rex operand;
        private final Rex index;

        private Impl(@NotNull Rex operand, @NotNull Rex index) {
            this.operand = operand;
            this.index = index;
        }

        @NotNull
        @Override
        public Rex getOperand() {
            return operand;
        }

        @NotNull
        @Override
        public Rex getIndex() {
            return index;
        }
    }
}
