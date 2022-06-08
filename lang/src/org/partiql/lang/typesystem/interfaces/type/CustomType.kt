package org.partiql.lang.typesystem.interfaces.type

/**
 * This interface is used to define a custom type
 */
abstract class CustomType : SqlType {
    override val isBuiltInType: Boolean
        get() = false
}
