package org.partiql.lang.typesystem.interfaces.type

/**
 * This interface is used to define a built-in type
 */
abstract class BuiltInType internal constructor() : SqlType {
    override val isBuiltInType: Boolean
        get() = true
}
