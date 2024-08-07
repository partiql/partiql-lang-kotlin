package org.partiql.plan.operator.rel

/**
 * PartiQL JOIN types.
 *
 *  - INNER, LEFT, RIGHT are LATERAL
 *  - FULL is NOT LATERAL
 */
public enum class RelJoinType {
    INNER,
    LEFT,
    RIGHT,
    FULL,
    OTHER;
}
