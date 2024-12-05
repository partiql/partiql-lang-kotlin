package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

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
     * @return operand rex (operand 0)
     */
    @NotNull
    public abstract Rex getOperand();

    /**
     * @return index rex
     */
    public abstract Rex getIndex();

    @Override
    @NotNull
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getOperand());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
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
