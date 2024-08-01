package org.partiql.plan.v1.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface Rex {

    public fun getType(): PType

    public fun getOperands(): List<Rex>

    public fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R
}
