package org.partiql.plan.operator.rel

import org.partiql.plan.operator.rex.RexVar

/**
 * TODO DOCUMENTATION
 */
public interface RelExcludePath {

    public fun getRoot(): RexVar

    public fun getSteps(): RelExcludeStep
}
