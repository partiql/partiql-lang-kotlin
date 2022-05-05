package org.partiql.lang.typesystem.interfaces.type

/**
 * This interface is used to define a primitive type
 */
abstract class PrimitiveType internal constructor() : BuiltInType() {
    override val isPrimitiveType: Boolean
        get() = true
}
