package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

import java.util.List;

/**
 * Abstract base class for all scalar expressions.
 */
public abstract class RexBase implements Rex {

    private int tag = 0;
    private List<Operator> operands;
    private RexType type;

    @Override
    public int getTag() {
        return tag;
    }

    @Override
    public void setTag(int tag) {
        this.tag = tag;
    }

    @NotNull
    @Override
    public final RexType getType() {
        if (type == null) {
            type = type();
        }
        return type;
    }

    @NotNull
    @Override
    public final Operator getOperand(int index) {
        return operands.get(index);
    }

    @NotNull
    @Override
    public final List<Operator> getOperands() {
        if (operands == null) {
            operands = operands();
        }
        return operands;
    }

    /**
     * PROTECTED (could also be package private atm).
     *
     * @return computed type.
     */
    protected abstract RexType type();

    /**
     * PROTECTED (could also be package private atm).
     *
     * @return computed operands.
     */
    protected abstract List<Operator> operands();
}
