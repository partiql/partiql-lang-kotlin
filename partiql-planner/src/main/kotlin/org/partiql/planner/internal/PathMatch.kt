package org.partiql.planner.internal

/**
 * Result of searching a path in a catalog.
 *
 * @param T
 * @property depth  The depth/level of the path match.
 * @property item   The metadata for the matched catalog item.
 */
internal data class PathMatch<T>(
    @JvmField val depth: Int,
    @JvmField val item: PathItem<T>,
)
