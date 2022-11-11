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
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.syntax.CORE_TYPE_NAME_ARITY_MAP

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
    val isRangedFrom: Boolean = false
) {
    MISSING(isUnknown = true),
    NULL(isUnknown = true),
    BOOL,
    INT(isNumber = true),
    FLOAT(isNumber = true),
    DECIMAL(isNumber = true),
    DATE,
    TIMESTAMP,
    TIME,
    SYMBOL(isText = true),
    STRING(isText = true),
    CLOB(isLob = true),
    BLOB(isLob = true),
    LIST(isSequence = true, isRangedFrom = true),
    SEXP(isSequence = true),
    STRUCT,
    BAG(isSequence = true, isRangedFrom = true);

    @Deprecated("Please use isUnknown instead", ReplaceWith("isUnknown"))
    fun isNull() = isUnknown

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

        private val LEX_TYPE_MAP = mapOf(
            *CORE_TYPE_NAME_ARITY_MAP.keys.map {
                val type = try {
                    ExprValueType.valueOf(it.toUpperCase())
                } catch (e: IllegalArgumentException) {
                    // no direct type mapping
                    when (it) {
                        "boolean" -> BOOL
                        "smallint", "integer", "integer4",
                        "integer8" -> INT
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
            ?: throw EvaluationException(
                "No such value type for $name",
                ErrorCode.LEXER_INVALID_NAME,
                internal = true
            )

        fun fromSqlDataType(sqlDataType: PartiqlAst.Type) = fromSqlDataTypeOrNull(sqlDataType)
            ?: throw EvaluationException(
                "No such ExprValueType for ${sqlDataType.javaClass.name}",
                ErrorCode.SEMANTIC_UNION_TYPE_INVALID,
                internal = true
            )

        fun fromSqlDataTypeOrNull(sqlDataType: PartiqlAst.Type) = when (sqlDataType) {
            is PartiqlAst.Type.BooleanType -> BOOL
            is PartiqlAst.Type.MissingType -> MISSING
            is PartiqlAst.Type.NullType -> NULL
            is PartiqlAst.Type.SmallintType -> INT
            is PartiqlAst.Type.Integer4Type -> INT
            is PartiqlAst.Type.Integer8Type -> INT
            is PartiqlAst.Type.IntegerType -> INT
            is PartiqlAst.Type.FloatType -> FLOAT
            is PartiqlAst.Type.RealType -> FLOAT
            is PartiqlAst.Type.DoublePrecisionType -> FLOAT
            is PartiqlAst.Type.DecimalType -> DECIMAL
            is PartiqlAst.Type.NumericType -> DECIMAL
            is PartiqlAst.Type.TimestampType -> TIMESTAMP
            is PartiqlAst.Type.CharacterType -> STRING
            is PartiqlAst.Type.CharacterVaryingType -> STRING
            is PartiqlAst.Type.StringType -> STRING
            is PartiqlAst.Type.SymbolType -> SYMBOL
            is PartiqlAst.Type.ClobType -> CLOB
            is PartiqlAst.Type.BlobType -> BLOB
            is PartiqlAst.Type.StructType -> STRUCT
            is PartiqlAst.Type.TupleType -> STRUCT
            is PartiqlAst.Type.ListType -> LIST
            is PartiqlAst.Type.SexpType -> SEXP
            is PartiqlAst.Type.BagType -> BAG
            is PartiqlAst.Type.AnyType -> null
            is PartiqlAst.Type.DateType -> DATE
            is PartiqlAst.Type.TimeType,
            is PartiqlAst.Type.TimeWithTimeZoneType -> TIME
            is PartiqlAst.Type.CustomType -> null
        }
    }
}
