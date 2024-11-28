package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical cast expression abstract base class.
 */
public abstract class RexCast extends RexBase {

    /**
     * @return new RexCast instance
     */
    @NotNull
    public static RexCast create(@NotNull Rex operand, @NotNull PType target) {
        return new Impl(operand, target);
    }

    /**
     * @return operand rex (child 0)
     */
    @NotNull
    public abstract Rex getOperand();

    /**
     * @return target type
     */
    @NotNull
    public abstract PType getTarget();

    @NotNull
    protected final RexType type() {
        return new RexType(getTarget());
    }

    @Override
    protected List<Operator> children() {
        return List.of();
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
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
