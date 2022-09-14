/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.IonType

/**
 * The core types of [ExprValue] that exist within the type system of the evaluator.
 * There is a correspondence to [IonType], but it isn't quite one-to-one.
 *
 * @param typeNames The normalized type names and aliases associated with the runtime type.
 * @param isRangedFrom Whether or not the `FROM` clause uses the value's iterator directly.
 */
enum class ExprValueType(
    val typeNames: List<String>,
    val isUnknown: Boolean = false,
    val isNumber: Boolean = false,
    val isText: Boolean = false,
    val isLob: Boolean = false,
    val isSequence: Boolean = false,
    val isRangedFrom: Boolean = false
) {
    MISSING(
        typeNames = listOf("missing"),
        isUnknown = true
    ),
    NULL(
        typeNames = listOf("null"),
        isUnknown = true
    ),
    BOOL(
        typeNames = listOf("bool", "boolean")
    ),
    INT(
        typeNames = listOf(
            "int", "smallint", "integer2", "int2", "integer", "integer4", "int4", "integer8", "int8",
            "bigint"
        ),
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
    DATE(
        typeNames = listOf("date")
    ),
    TIMESTAMP(
        typeNames = listOf("timestamp")
    ),
    TIME(
        typeNames = listOf("time")
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

    @Deprecated("Please use isUnknown instead", ReplaceWith("isUnknown"))
    fun isNull() = isUnknown

    /** The canonical PartiQL textual names for the runtime type. */
    val sqlTextNames = typeNames.map { it.toUpperCase().replace("_", " ") }

    /** Whether or not the given type is in the same type grouping as another. */
    fun isDirectlyComparableTo(other: ExprValueType): Boolean =
        (this == other) ||
            (isNumber && other.isNumber) ||
            (isText && other.isText) ||
            (isLob && other.isLob)

    companion object {
        private val ION_TYPE_MAP = enumValues<IonType>().asSequence()
            .map {
                val ourType = when (it) {
                    IonType.DATAGRAM -> BAG
                    else -> valueOf(it.name)
                }
                Pair(it, ourType)
            }.toMap()

        /** Maps an [IonType] to an [ExprType]. */
        fun fromIonType(ionType: IonType): ExprValueType = ION_TYPE_MAP[ionType]!!
    }
}
