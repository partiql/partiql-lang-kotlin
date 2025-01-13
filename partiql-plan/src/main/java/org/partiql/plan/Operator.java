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
    int getTag();

    /**
     * Tag setter.
     *
     * @param tag new tag value.
     */
    void setTag(int tag);

    /**
     * Visitor accept.
     *
     * @param visitor visitor implementation.
     * @param ctx     visitor scoped args.
     * @param <R>     Visitor return type.
     * @param <C>     Visitor context type (scoped args).
     * @return R
     */
    <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx);

    /**
     * @return the i-th operand
     */
    Operand getOperand(int index);

    /**
     * @return all input operands.
     */
    @NotNull
    List<Operand> getOperands();
}
