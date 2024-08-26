package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.operator.rex.RexVar

/**
 * TODO DOCUMENTATION
 */
public interface RelExcludePath {

    public fun getRoot(): RexVar

    public fun getSteps(): RelExcludeStep
}
