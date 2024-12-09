package org.partiql.planner.internal

internal enum class PlannerFlag {
    /**
     * Determine the planner behavior upon encounter an operation that always returns MISSING.
     *
     * If this flag is included:
     *
     *    The problematic operation will be tracked in problem callback as a error.
     *
     *    The result plan will turn the problematic operation into an error node.
     *
     * Otherwise:
     *
     *    The problematic operation will be tracked in problem callback as a missing.
     *
     *    The result plan will turn the problematic operation into a missing node.
     */
    SIGNAL_MODE
}
