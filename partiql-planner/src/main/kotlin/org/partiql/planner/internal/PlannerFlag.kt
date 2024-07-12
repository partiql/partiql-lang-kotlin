package org.partiql.planner.internal

// Marker interface
internal interface PlannerFlag

internal enum class BooleanFlag : PlannerFlag {
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
    SIGNAL_MODE,

    /**
     * Determines lvalue behavior
     *
     * If this flag is included:
     *    Lvalue (AS binding, DDL, etc) are case-sensitive
     *
     * If not included:
     *   Lvalue are normalized using an implementation defined normalization rule
     */
    CASE_PRESERVATION
}

internal enum class RValue : PlannerFlag {
    /**
     * Using upper case text to look up
     */
    FOLDING_UP,

    /**
     * Using lower case text to look up
     */
    FOLDING_DOWN,

    /**
     * Using original text to look up
     */
    SENSITIVE,

    /**
     * Match behavior: text comparison with case ignored.
     */
    INSENSITIVE
}
