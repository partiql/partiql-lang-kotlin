package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelFilter : Rel {

    public fun getInput(): Rel

    public fun getPredicate(): Rex
}
