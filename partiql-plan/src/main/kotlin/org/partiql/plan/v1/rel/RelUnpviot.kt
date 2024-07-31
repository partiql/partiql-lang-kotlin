package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelUnpviot : Rel {

    public fun getInput(): Rex
}
