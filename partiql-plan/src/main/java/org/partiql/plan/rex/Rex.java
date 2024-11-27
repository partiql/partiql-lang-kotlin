package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

/**
 * A [Rex] is an [Operator] that produces a value.
 */
public interface Rex extends Operator {

    @NotNull
    public RexType getType();
}
