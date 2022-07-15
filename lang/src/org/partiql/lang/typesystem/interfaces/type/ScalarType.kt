package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a scalar type
 */
interface ScalarType {
    /**
     * Type aliases
     *
     * A type can have multiple type aliases. Within one plugin,
     * one type alias can only be used once across types
     */
    val typeAliases: List<String>

    /**
     * Run-time type
     */
    val exprValueType: ExprValueType
}
