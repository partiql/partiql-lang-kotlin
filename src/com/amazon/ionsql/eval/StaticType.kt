/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * Represent static types available in the language and ways to extends them to create new types
 *
 * @param name name of the type
 */
sealed class StaticType(val name: String) {
    companion object {

        /**
         * varargs variant, folds [types] into a [Set]
         */
        @JvmStatic
        fun unionOf(name: String, vararg types: StaticType) = unionOf(name, types.toHashSet())

        /**
         * Creates a new [StaticType] as a union of the passed [types]. The values typed by the returned type
         * are defined as the union of all values typed as [types]
         *
         * @param types [StaticType] to be united, needs to have at least one element
         * @return [StaticType] representing the union of [types]
         */
        @JvmStatic
        fun unionOf(name: String, types: Set<StaticType>): StaticType =
            when (types.size) {
                0 -> err("types can not be empty")
                else -> TaggedUnion(name, types)
            }

        // direct from ExprValueType types
        @JvmField val MISSING: StaticType = SingleType("Missing", ExprValueType.MISSING)
        @JvmField val BOOL: StaticType = SingleType("Bool", ExprValueType.BOOL)
        @JvmField val NULL: StaticType = SingleType("Null", ExprValueType.NULL)
        @JvmField val INT: StaticType = SingleType("Int", ExprValueType.INT)
        @JvmField val FLOAT: StaticType = SingleType("Float", ExprValueType.FLOAT)
        @JvmField val DECIMAL: StaticType = SingleType("Decimal", ExprValueType.DECIMAL)
        @JvmField val TIMESTAMP: StaticType = SingleType("Timestamp", ExprValueType.TIMESTAMP)
        @JvmField val SYMBOL: StaticType = SingleType("Symbol", ExprValueType.SYMBOL)
        @JvmField val STRING: StaticType = SingleType("String", ExprValueType.STRING)
        @JvmField val CLOB: StaticType = SingleType("Clob", ExprValueType.CLOB)
        @JvmField val BLOB: StaticType = SingleType("Blob", ExprValueType.BLOB)
        @JvmField val LIST: StaticType = SingleType("List", ExprValueType.LIST)
        @JvmField val SEXP: StaticType = SingleType("Sexp", ExprValueType.SEXP)
        @JvmField val STRUCT: StaticType = SingleType("Struct", ExprValueType.STRUCT)
        @JvmField val BAG: StaticType = SingleType("Bag", ExprValueType.BAG)

        // union types
        @JvmField val NUMERIC: StaticType = StaticType.unionOf("Numeric", INT, FLOAT, DECIMAL)
        @JvmField val ANY: StaticType = StaticType.unionOf("Any", MISSING, BOOL, NULL, INT, FLOAT, DECIMAL, TIMESTAMP, SYMBOL, STRING, CLOB, BLOB, LIST, SEXP, STRUCT, BAG)
    }

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
