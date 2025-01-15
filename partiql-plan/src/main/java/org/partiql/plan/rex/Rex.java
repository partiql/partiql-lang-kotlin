package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

/**
 * A {@link Rex} is an {@link Operator} that produces a value.
 */
public interface Rex extends Operator {

    /**
     * Gets the type of the value produced by this rex.
     * @return the type of the value produced by this rex.
     */
    @NotNull
    public RexType getType();

    /**
     * Sets the type of the value produced by this rex.
     * @param type the new type of the value produced by this rex.
     */
    public void setType(RexType type);
}
