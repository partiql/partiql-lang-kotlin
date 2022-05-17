package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a sql type
 */
interface SqlType {
    /**
     * Type name
     *
     * A sql type must have a unique type name within a plugin
     */
    val typeName: String

    /**
     * Run-time type
     */
    val exprValueType: ExprValueType

    /**
     * Its parent type in PartiQL. Null value means no parent type.
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
