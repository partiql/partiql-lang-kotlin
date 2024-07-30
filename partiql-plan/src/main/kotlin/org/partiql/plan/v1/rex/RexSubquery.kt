package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * TODO DOCUMENTATION
 */
public interface RexSubquery : Rex {

    public fun getInput(): Rel
}
