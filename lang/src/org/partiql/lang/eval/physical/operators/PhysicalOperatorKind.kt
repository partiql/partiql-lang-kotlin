package org.partiql.lang.eval.physical.operators

/**
 * Indicates which physical relational operator a [PhysicalOperatorFactory] can create instances of.
 *
 * This is part of [PhysicalOperatorFactoryKey] and also implies which subclass of [PhysicalOperatorFactory] the
 * implementation must derive from; e.g. An [PhysicalOperatorFactory]s with [PROJECT] in its key must implement
 * [ProjectPhysicalOperatorFactory], with [FILTER] the factory must implement [FilterPhysicalOperatorFactory], and so
 * on.
 */
enum class PhysicalOperatorKind {
    PROJECT,
    SCAN,
    FILTER,
    JOIN,
    OFFSET,
    LIMIT,
    LET
}
