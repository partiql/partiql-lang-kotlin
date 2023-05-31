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
 * @param isRangedFrom Whether or not the `FROM` clause uses the value's iterator directly.
 */
enum class ExprValueType(
    val isUnknown: Boolean = false,
    val isNumber: Boolean = false,
    val isText: Boolean = false,
    val isLob: Boolean = false,
    val isSequence: Boolean = false,
    val isRangedFrom: Boolean = false,
    val isScalar: Boolean = false
) {
    MISSING(isUnknown = true),
    NULL(isUnknown = true),
    BOOL(isScalar = true),
    INT(isNumber = true, isScalar = true),
    FLOAT(isNumber = true, isScalar = true),
    DECIMAL(isNumber = true, isScalar = true),
    DATE(isScalar = true),
    TIMESTAMP(isScalar = true),
    TIME(isScalar = true),
    SYMBOL(isText = true, isScalar = true),
    STRING(isText = true, isScalar = true),
    CLOB(isLob = true, isScalar = true),
    BLOB(isLob = true, isScalar = true),
    LIST(isSequence = true, isRangedFrom = true),
    SEXP(isSequence = true),
    STRUCT,
    BAG(isSequence = true, isRangedFrom = true),
    GRAPH;

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

        /** Maps an [IonType] to an [ExprValueType]. */
        fun fromIonType(ionType: IonType): ExprValueType = ION_TYPE_MAP[ionType]!!
    }
}
