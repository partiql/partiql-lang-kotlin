package org.partiql.plan

/**
 * PartiQL JOIN types.
 *
 * TODO use 1.0 enum modeling.
 */
public enum class JoinType {
    INNER,
    LEFT,
    RIGHT,
    FULL,
}
