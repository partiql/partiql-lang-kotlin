package org.partiql.plan.v1.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexCast : Rex {

    public fun getOperand(): Rex

    public fun getTarget(): PType
}
