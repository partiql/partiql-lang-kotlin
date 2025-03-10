package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical cast expression abstract base class.
 */
public abstract class RexCast extends RexBase {

    /**
     * Creates a new cast instance.
     * @param operand operand rex (operand 0)
     * @param target target type
     * @return new cast instance
     */
    @NotNull
    public static RexCast create(@NotNull Rex operand, @NotNull PType target) {
        return new Impl(operand, target);
    }

    /**
     * Gets the operand rex.
     * @return operand rex (operand 0)
     */
    @NotNull
    public abstract Rex getOperand();

    /**
     * Gets the target type.
     * @return target type
     */
    @NotNull
    public abstract PType getTarget();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(getTarget());
    }

    @NotNull
    @Override
    protected List<Operand> operands() {
        Operand c0 = Operand.single(getOperand());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitCast(this, ctx);
    }

    private static class Impl extends RexCast {
        private final Rex operand;
        private final PType target;

        private Impl(@NotNull Rex operand, @NotNull PType target) {
            this.operand = operand;
            this.target = target;
        }

        @NotNull
        @Override
        public Rex getOperand() {
            return operand;
        }

        @NotNull
        @Override
        public PType getTarget() {
            return target;
        }
    }
}
