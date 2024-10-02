package org.partiql.plan.rel

import org.partiql.plan.Schema

/**
 * TODO DOCUMENTATION
 */
public interface Rel {

    public fun getSchema(): Schema

    public fun isOrdered(): Boolean

    public fun getChildren(): Collection<Rel>

    public fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R
}
