package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;

import java.util.List;

/**
 * Abstract base class for all relational operators.
 */
public abstract class RelBase implements Rel {

    private int tag = 0;
    private RelType type;
    private List<Operand> operands;

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
    public final RelType getType() {
        if (type == null) {
            type = type();
        }
        return type;
    }

    @Override
    public void setType(@NotNull RelType type) {
        this.type = type;
    }

    @NotNull
    @Override
    public final Operand getOperand(int index) {
        if (operands == null) {
            operands = operands();
        }
        return operands.get(index);
    }

    @NotNull
    @Override
    public final List<Operand> getOperands() {
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
    @NotNull
    protected abstract RelType type();

    /**
     * PROTECTED (could also be package private atm).
     *
     * @return computed operands.
     */
    @NotNull
    protected abstract List<Operand> operands();
}
