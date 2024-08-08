package org.partiql.plan.operator.rel

import org.partiql.plan.Schema

/**
 * TODO DOCUMENTATION
 */
public interface Rel {

    public fun getInputs(): List<Rel>

    public fun getSchema(): Schema

    public fun isOrdered(): Boolean

    public fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R
}
