package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * TODO DOCUMENTATION
 */
public interface RexPivot : Rex {

    public fun getInput(): Rel

    public fun getKey(): Rex

    public fun getValue(): Rex
}
