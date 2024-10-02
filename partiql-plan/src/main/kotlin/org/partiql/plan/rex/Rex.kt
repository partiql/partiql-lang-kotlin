package org.partiql.plan.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface Rex {

    public fun getType(): PType

    public fun getChildren(): Collection<Rex>

    public fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R
}
