package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a sql type
 */
abstract class SqlType internal constructor() : Type {
    /**
     * A sql type has a unique type name
     */
    abstract val typeName: String

    /**
     * Run-time type
     */
    abstract val exprValueType: ExprValueType

    /**
     * Whether it is a primitive type
     */
    abstract val isPrimitiveType: Boolean

    /**
     * Whether it is a built-in type
     */
    abstract val isBuiltInType: Boolean
}
