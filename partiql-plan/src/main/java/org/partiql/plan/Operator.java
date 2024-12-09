package org.partiql.plan;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The interface for all logical plan operators.
 */
public interface Operator {

    /**
     * Tag getter.
     */
    public int getTag();

    /**
     * Tag setter.
     *
     * @param tag new tag value.
     */
    public void setTag(int tag);

    /**
     * Visitor accept.
     *
     * @param visitor visitor implementation.
     * @param ctx     visitor scoped args.
     * @param <R>     Visitor return type.
     * @param <C>     Visitor context type (scoped args).
     * @return R
     */
    public abstract <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx);

    /**
     * @return the i-th operand
     */
    public abstract Operand getOperand(int index);

    /**
     * @return all input operands.
     */
    @NotNull
    public abstract List<Operand> getOperands();
}
