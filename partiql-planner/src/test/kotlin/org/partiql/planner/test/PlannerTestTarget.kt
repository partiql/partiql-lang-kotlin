package org.partiql.planner.test

public abstract class PlannerTestTarget(
    public val name: String,
    public val suite: String,
) {

    /**
     * Perform target-specific assertion.
     *
     * @param test
     */
    public abstract fun assert(test: PlannerTest)
}
