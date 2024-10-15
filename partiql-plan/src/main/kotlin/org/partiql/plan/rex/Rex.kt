package org.partiql.plan.rex

import org.partiql.plan.Operator

/**
 * TODO DOCUMENTATION
 */
public interface Rex : Operator {

    public fun getType(): RexType
}
