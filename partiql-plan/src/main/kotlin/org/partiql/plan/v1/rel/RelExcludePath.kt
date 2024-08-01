package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.RexVar

/**
 * TODO DOCUMENTATION
 */
public interface RelExcludePath {

    public fun getRoot(): RexVar

    public fun getSteps(): RelExcludeStep
}
