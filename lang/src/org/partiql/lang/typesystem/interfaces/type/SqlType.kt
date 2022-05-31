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
     * Its parent sql type. Null value means no parent sql type
     *
     * When we define a sql function or operator to take arguments of
     * a certain type, its subtypes are also considered as valid as
     * the argument type
     */
    val parentType: SqlType?

    /**
     * Whether it is a primitive type
     */
    val isPrimitiveType: Boolean

    /**
     * Whether it is a built-in type
     */
    val isBuiltInType: Boolean
}
