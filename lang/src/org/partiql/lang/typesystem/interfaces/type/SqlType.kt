package org.partiql.lang.typesystem.interfaces.type

import org.partiql.lang.eval.ExprValueType

/**
 * This interface is used to define a sql type
 */
abstract class SqlType internal constructor() : Type {
    /**
     * A sql type has a unique type name
     */
    abstract fun getTypeName(): String

    /**
     * Run-time type
     */
    abstract fun getExprValueType(): ExprValueType

    /**
     * Whether it is a primitive type
     */
    abstract fun isPrimitiveType(): Boolean

    /**
     * Whether it is a built-in type
     */
    abstract fun isBuiltInType(): Boolean
}
