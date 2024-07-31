package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex

/**
 * TODO DOCUMENTATION
 */
public interface RelProject : Rel {

    public fun getInput(): Rel

    public fun getProjections(): List<Rex>
}
