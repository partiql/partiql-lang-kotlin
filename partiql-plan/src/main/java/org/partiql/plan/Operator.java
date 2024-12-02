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
     * Get i-th child (input) operator.
     *
     * @param index child index
     * @return child operator
     */
    @NotNull
    public abstract Operator getOperand(int index);

    /**
     * @return all child (input) operators.
     */
    @NotNull
    public abstract List<Operator> getOperands();
}
