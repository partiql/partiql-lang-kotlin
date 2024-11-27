package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.types.PType;

/**
 * Logical cast expression abstract base class.
 */
public abstract class RexCast extends RexBase {

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
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitCast(this, ctx);
    }
}
