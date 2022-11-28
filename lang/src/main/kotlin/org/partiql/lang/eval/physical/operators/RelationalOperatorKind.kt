package org.partiql.lang.eval.physical.operators

/**
 * Indicates which physical relational operator a [RelationalOperatorFactory] can create instances of.
 *
 * This is part of [RelationalOperatorFactoryKey] and also implies which subclass of [RelationalOperatorFactory] the
 * implementation must derive from; e.g. a [RelationalOperatorFactory]s with [PROJECT] in its key must implement
 * [ProjectRelationalOperatorFactory], with [FILTER] the factory must implement [FilterRelationalOperatorFactory], and
 * so on.
 */
enum class RelationalOperatorKind {
    PROJECT,
    SCAN,
    UNPIVOT,
    FILTER,
    JOIN,
    WINDOW,
    OFFSET,
    LIMIT,
    LET,
    SORT,
    AGGREGATE
}
