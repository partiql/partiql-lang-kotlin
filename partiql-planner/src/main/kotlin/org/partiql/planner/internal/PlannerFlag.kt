package org.partiql.planner.internal

internal enum class PlannerFlag {
    /**
     * Determine the planner behavior upon encounter an operation that always returns MISSING.
     *
     * If this flag is included:
     *
     *    The problematic operation will be tracked in problem callback as an error.
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
     * Experimental flag to enable planner to replace references to WITH variables with their definitions.
     * By default, this flag is included in the default planner phase.
     */
    FORCE_INLINE_WITH_CLAUSE,

    /**
     * When set, the planner emits integer-referenced table nodes (RexTableRef) instead of embedding
     * live Table objects (RexTable). These plans are thread-safe, cacheable, and executable via PartiQLVM.
     *
     * Functions and aggregates are always embedded directly (they are stateless/thread-safe).
     * Only tables need ref-based resolution since their data may change between executions.
     */
    USE_REFS
}
