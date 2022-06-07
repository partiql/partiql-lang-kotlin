package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a sql type
 */
interface SqlType {
    /**
     * Type aliases
     *
     * A sql type can have multiple type aliases. Within one plugin,
     * one type alias can only be used once across types
     */
    val typeAliases: List<String>

    /**
     * Run-time type
     */
    val exprValueType: ExprValueType

    /**
     * Whether it is a primitive type. A primitive type corresponds to one [ExprValueType]
     */
    val isPrimitiveType: Boolean

    /**
     * Whether it is a built-in type
     */
    val isBuiltInType: Boolean
}
