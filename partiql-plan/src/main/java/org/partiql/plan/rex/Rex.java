package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

/**
 * A [Rex] is an [Operator] that produces a value.
 */
public interface Rex extends Operator {

    /**
     * @return the type of the value produced by this rex.
     */
    @NotNull
    public RexType getType();

    /**
     * @param type the new type of the value produced by this rex.
     */
    public void setType(RexType type);
}
