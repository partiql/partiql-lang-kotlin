/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonType
import com.amazon.ionsql.syntax.TYPE_NAME_ARITY_MAP

/**
 * The core types of [ExprValue] that exist within the type system of the evaluator.
 * There is a correspondence to [IonType], but it isn't quite one-to-one.
 */
enum class ExprValueType {
    MISSING,
    NULL,
    BOOL,
    INT,
    FLOAT,
    DECIMAL,
    TIMESTAMP,
    SYMBOL,
    STRING,
    CLOB,
    BLOB,
    LIST,
    SEXP,
    STRUCT,
    BAG;

    companion object {
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
                        "smallint", "integer" -> INT
                        "real", "double_precision" -> FLOAT
                        "numeric" -> DECIMAL
                        "character", "character_varying" -> STRING
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
