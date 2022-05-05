package org.partiql.lang.typesystem.interfaces.type

/**
 * Used to define an abstract type, such as ANY, NUMBER.
 *
 * Basically, an abstract type is a collection of multiple sql types. e.g. ANY is a
 * union of all the primitive sql types, NUMBER is a union of INT, DECIMAL &
 * FLOAT, etc. It can be used to represent the type of function/operator arguments,
 * however, it should not be assigned to a value (not a data type). Thus, it does not
 * have a corresponding run-time type.
 */
abstract class AbstractType internal constructor() : Type {
    /**
     * Get the corresponding sql types
     */
    abstract val sqlTypes: List<SqlType>
}
