package org.partiql.lang.typesystem.interfaces.operator

/**
 * All the PartiQL operators listed here.
 */
enum class OpAlias {
    NOT, POS, NEG, PLUS, MINUS, TIMES, DIVIDE,
    MODULO, CONCAT, AND, OR, EQ, NE, GT, GTE,
    LT, LTE, LIKE, BETWEEN, IN, IS, CAST,
    CAN_CAST, CAN_LOSSLESS_CAST, UNION, EXCEPT,
    INTERSECT
}
