package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
interface RelOffset : Rel {

    public fun getInput(): Rel

    public fun getLimit(): Rex
}
