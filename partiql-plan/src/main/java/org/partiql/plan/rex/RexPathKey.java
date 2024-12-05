package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * Logical path by key lookup expression abstract base class.
 */
public abstract class RexPathKey extends RexBase {

    /**
     * @return new RexPathKey instance
     */
    @NotNull
    public static RexPathKey create(@NotNull Rex operand, @NotNull Rex key) {
        return new Impl(operand, key);
    }

    /**
     * @return operand rex (operand 0)
     */
    @NotNull
    public abstract Rex getOperand();

    /**
     * @return key rex.
     */
    @NotNull
    public abstract Rex getKey();

    @Override
    @NotNull
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected List<Operand> operands() {
        Operand c0 = Operand.single(getOperand());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitPathKey(this, ctx);
    }

    private static class Impl extends RexPathKey {

        private final Rex operand;
        private final Rex key;

        private Impl(@NotNull Rex operand, @NotNull Rex key) {
            this.operand = operand;
            this.key = key;
        }

        @NotNull
        @Override
        public Rex getOperand() {
            return operand;
        }

        @NotNull
        @Override
        public Rex getKey() {
            return key;
        }
    }
}
