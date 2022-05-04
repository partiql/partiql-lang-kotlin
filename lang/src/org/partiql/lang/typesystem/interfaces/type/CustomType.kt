package org.partiql.lang.typesystem.interfaces.type

/**
 * This interface is used to define a custom type
 */
abstract class CustomType: SqlType() {
    override fun isBuiltInType(): Boolean = false

    override fun isPrimitiveType(): Boolean = false
}