/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.types

import org.partiql.lang.eval.ExprValueType

// TODO consider refactoring this out as a proper interface instead of a sealed class to allow us to enumerate base types.

/**
 * Represents static types available in the language and ways to extends them to create new types.
 *
 * @param name name of the type
 */
sealed class StaticType(val name: String) {
    companion object {

        private val EXPR_VALUE_TYPE_MAP =
            ExprValueType.values().map {
                it to SingleType(it.name, it)
            }.toMap()

        /**
         * varargs variant, folds [types] into a [Set]
         */
        @JvmStatic
        fun unionOf(name: String, vararg types: StaticType) = unionOf(name, types.toHashSet())

        /**
         * Creates a new [StaticType] as a union of the passed [types]. The values typed by the returned type
         * are defined as the union of all values typed as [types]
         *
         * @param types [StaticType] to be unioned.
         * @return [StaticType] representing the union of [types]
         */
        @JvmStatic
        fun unionOf(name: String, types: Set<StaticType>): StaticType = TaggedUnion(name, types)

        // TODO consider making these into an enumeration...
        
        // direct from ExprValueType types
        @JvmField val MISSING: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.MISSING)
        @JvmField val BOOL: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.BOOL)
        @JvmField val NULL: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.NULL)
        @JvmField val INT: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.INT)
        @JvmField val FLOAT: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.FLOAT)
        @JvmField val DECIMAL: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.DECIMAL)
        @JvmField val TIMESTAMP: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.TIMESTAMP)
        @JvmField val SYMBOL: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.SYMBOL)
        @JvmField val STRING: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.STRING)
        @JvmField val CLOB: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.CLOB)
        @JvmField val BLOB: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.BLOB)
        @JvmField val LIST: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.LIST)
        @JvmField val SEXP: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.SEXP)
        @JvmField val STRUCT: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.STRUCT)
        @JvmField val BAG: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.BAG)
        @JvmField val DATE: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.DATE)
        @JvmField val TIME: StaticType = EXPR_VALUE_TYPE_MAP.getValue(ExprValueType.TIME)

        private val NUMERIC_NAME = "NUMERIC"
        private val ANY_NAME = "ANY"
        private val NOTHING_NAME = "NOTHING"

        // union types
        @JvmField val NUMERIC: StaticType = unionOf(NUMERIC_NAME,
            INT, FLOAT, DECIMAL)
        @JvmField val ANY: StaticType = unionOf(ANY_NAME,
            MISSING, BOOL, NULL, INT, FLOAT, DECIMAL, TIMESTAMP, SYMBOL, STRING, CLOB, BLOB,
            LIST, SEXP, STRUCT, BAG)
        @JvmField val NOTHING: StaticType = unionOf(NOTHING_NAME)

        private val STANDARD_TYPES = EXPR_VALUE_TYPE_MAP.values + listOf(NUMERIC, ANY, NOTHING)

        private val NAME_STANDARD_STATIC_TYPE_MAP = STANDARD_TYPES.map { it.name to it }.toMap()

        /**
         * Returns the static type specified by the given name.
         */
        @JvmStatic
        fun fromTypeName(name: String): StaticType =
            NAME_STANDARD_STATIC_TYPE_MAP[name]
                ?: throw IllegalArgumentException("No such built in type named: $name")
    }

    // TODO add the concept of container type.

    /**
     * Checks if the passed [ExprValueType] types as this [StaticType].
     *
     * **Example**: A function that has one parameter of [StaticType] A can only be called during evaluation with an
     * argument of [ExprValueType] B that satisfies `A.isOfType(B) == true`
     *
     * @param exprValueType [ExprValueType] to check
     * @return true if this [StaticType] types the argument
     */
    abstract fun isOfType(exprValueType: ExprValueType): Boolean

    abstract val typeDomain: Set<ExprValueType>

    override fun toString(): String {
        return name
    }
}

/**
 * Represents a [StaticType] that is type of a single [ExprValueType]
 */
private class SingleType(name: String, val type: ExprValueType) : StaticType(name) {
    override val typeDomain = setOf(type)

    override fun isOfType(exprValueType: ExprValueType): Boolean = type == exprValueType
}

/**
 * Represents a [StaticType] that's defined by the union of multiple [StaticType]s. Types any [ExprValueType] that
 * its elements type
 */
private class TaggedUnion(name: String, val types: Set<StaticType>) : StaticType(name) {
    override val typeDomain = types.flatMap { it.typeDomain }.toSet()

    override fun isOfType(exprValueType: ExprValueType): Boolean = types.any { it.isOfType(exprValueType) }
}
