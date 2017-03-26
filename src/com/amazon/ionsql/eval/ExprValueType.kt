/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonType
import com.amazon.ionsql.syntax.TYPE_ALIASES
import com.amazon.ionsql.syntax.TYPE_NAME_ARITY_MAP

/**
 * The core types of [ExprValue] that exist within the type system of the evaluator.
 * There is a correspondence to [IonType], but it isn't quite one-to-one.
 *
 * @param typeNames The normalized type names and aliases associated with the runtime type.
 * @param isRangedFrom Whether or not the `FROM` clause uses the value's iterator directly.
 */
enum class ExprValueType(val typeNames: List<String>,
                         val isNull: Boolean = false,
                         val isNumber: Boolean = false,
                         val isText: Boolean = false,
                         val isLob: Boolean = false,
                         val isSequence: Boolean = false,
                         val isRangedFrom: Boolean = false) {
    MISSING(
        typeNames = listOf("missing"),
        isNull = true
    ),
    NULL(
        typeNames = listOf("null"),
        isNull = true
    ),
    BOOL(
        typeNames = listOf("bool", "boolean")
    ),
    INT(
        typeNames = listOf("int", "smallint", "integer"),
        isNumber = true
    ),
    FLOAT(
        typeNames = listOf("float", "real", "double_precision"),
        isNumber = true
    ),
    DECIMAL(
        typeNames = listOf("dec", "decimal", "numeric"),
        isNumber = true
    ),
    TIMESTAMP(
        typeNames = listOf("timestamp")
    ),
    SYMBOL(
        typeNames = listOf("symbol"),
        isText = true
    ),
    STRING(
        typeNames = listOf("string", "char", "varchar", "character", "character_varying"),
        isText = true
    ),
    CLOB(
        typeNames = listOf("clob"),
        isLob = true
    ),
    BLOB(
        typeNames = listOf("blob"),
        isLob = true
    ),
    LIST(
        typeNames = listOf("list"),
        isSequence = true,
        isRangedFrom = true
    ),
    SEXP(
        typeNames = listOf("sexp"),
        isSequence = true
    ),
    STRUCT(
        typeNames = listOf("struct", "tuple")
    ),
    BAG(
        typeNames = listOf("bag"),
        isSequence = true,
        isRangedFrom = true
    );

    /** The canonical SQL++ textual names for the runtime type. */
    val sqlTextNames = typeNames.map { it.toUpperCase().replace("_", " ") }

    companion object {
        init {
            // validate that this enum is consistent with the normalized type names and aliases
            val lexerTypeNames = TYPE_NAME_ARITY_MAP.keys union TYPE_ALIASES.keys
            val declaredTypeNames = mutableSetOf<String>()
            values().flatMap { it.typeNames }.forEach {
                if (it !in lexerTypeNames) {
                    throw IllegalStateException("Declared type name does not exist in lexer: $it")
                }
                if (it in declaredTypeNames) {
                    throw IllegalStateException("Duplicate declaration for $it")
                }
                declaredTypeNames.add(it)
            }
            val undeclaredTypeNames = lexerTypeNames - declaredTypeNames
            if (undeclaredTypeNames.isNotEmpty()) {
                throw IllegalStateException("Undeclared type names: $undeclaredTypeNames")
            }
        }

        private val ION_TYPE_MAP = IonType.values().asSequence()
            .map {
                val ourType = when (it) {
                    IonType.DATAGRAM -> BAG
                    else -> valueOf(it.name)
                }
                Pair(it, ourType)
            }.toMap()

        /** Maps an [IonType] to an [ExprType]. */
        fun fromIonType(ionType: IonType): ExprValueType = ION_TYPE_MAP[ionType]!!

        private val LEX_TYPE_MAP = mapOf(
            *TYPE_NAME_ARITY_MAP.keys.map {
                val type = try {
                    ExprValueType.valueOf(it.toUpperCase())
                } catch (e: IllegalArgumentException) {
                    // no direct type mapping
                    when (it) {
                        "boolean" -> BOOL
                        "smallint", "integer" -> INT
                        "real", "double_precision" -> FLOAT
                        "numeric" -> DECIMAL
                        "character", "character_varying" -> STRING
                        "tuple" -> STRUCT
                        else -> throw IllegalStateException("No ExprValueType handler for $it")
                    }
                }

                Pair(it, type)
            }.toTypedArray()
        )

        fun fromTypeName(name: String): ExprValueType = LEX_TYPE_MAP[name]
            ?: throw EvaluationException("No such value type for $name")
    }
}
