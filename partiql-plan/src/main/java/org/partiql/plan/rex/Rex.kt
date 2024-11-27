package org.partiql.plan.rex

import org.partiql.plan.Operator

/**
 * A [Rex] is an [Operator] that produces a value.
 */
public interface Rex : Operator {

    public fun getType(): RexType
}
