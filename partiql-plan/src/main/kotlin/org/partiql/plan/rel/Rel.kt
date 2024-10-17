package org.partiql.plan.rel

import org.partiql.plan.Operator

/**
 * A [Rel] is an [Operator] that produces a collection of tuples.
 */
public interface Rel : Operator {

    public fun getType(): RelType

    public fun isOrdered(): Boolean
}
