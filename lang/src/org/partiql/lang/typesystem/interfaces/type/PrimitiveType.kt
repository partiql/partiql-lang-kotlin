package org.partiql.lang.typesystem.interfaces.type

/**
 * This interface is used to define a primitive type
 */
abstract class PrimitiveType internal constructor() : BuiltInType() {
    override fun isPrimitiveType(): Boolean = true
}
