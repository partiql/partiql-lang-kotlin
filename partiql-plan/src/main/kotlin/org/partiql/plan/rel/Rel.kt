package org.partiql.plan.rel

import org.partiql.plan.Operator

/**
 * TODO DOCUMENTATION
 */
public interface Rel : Operator {

    public fun getType(): RelType

    public fun isOrdered(): Boolean
}
