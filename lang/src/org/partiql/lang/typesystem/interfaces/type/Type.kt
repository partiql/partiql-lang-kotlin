package org.partiql.lang.typesystem.interfaces.type

/**
 * A type in PartiQL
 */
interface Type {
    /**
     * Its parent type in PartiQL. Null value means no parent type.
     */
    fun getParentType(): Type?
}