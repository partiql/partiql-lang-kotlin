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
import org.partiql.lang.ast.*
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAst.Type.*
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.syntax.TYPE_ALIASES
import org.partiql.lang.syntax.CORE_TYPE_NAME_ARITY_MAP

/**
 * The core types of [ExprValue] that exist within the type system of the evaluator.
 * There is a correspondence to [IonType], but it isn't quite one-to-one.
 *
 * @param typeNames The normalized type names and aliases associated with the runtime type.
 * @param isRangedFrom Whether or not the `FROM` clause uses the value's iterator directly.
 */
enum class ExprValueType(val typeNames: List<String>,
                         val isUnknown: Boolean = false,
                         val isNumber: Boolean = false,
                         val isText: Boolean = false,
                         val isLob: Boolean = false,
                         val isSequence: Boolean = false,
                         val isRangedFrom: Boolean = false) {
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
        typeNames = listOf("int", "smallint", "integer2", "int2", "integer", "integer4", "int4", "integer8", "int8",
                "bigint"),
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
        (this == other)
            || (isNumber && other.isNumber)
            || (isText && other.isText)
            || (isLob && other.isLob)

    companion object {
        init {
            // validate that this enum is consistent with the normalized type names and aliases
            val lexerTypeNames = (CORE_TYPE_NAME_ARITY_MAP.keys union TYPE_ALIASES.keys)
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

        private val ION_TYPE_MAP = enumValues<IonType>().asSequence()
            .map {
                val ourType = when (it) {
                    IonType.DATAGRAM -> BAG
                    else             -> valueOf(it.name)
                }
                Pair(it, ourType)
            }.toMap()

        /** Maps an [IonType] to an [ExprType]. */
        fun fromIonType(ionType: IonType): ExprValueType = ION_TYPE_MAP[ionType]!!

        private val LEX_TYPE_MAP = mapOf(
            *CORE_TYPE_NAME_ARITY_MAP.keys.map {
                val type = try {
                    ExprValueType.valueOf(it.toUpperCase())
                } catch (e: IllegalArgumentException) {
                    // no direct type mapping
                    when (it) {
                        "boolean"                                    -> BOOL
                        "smallint", "integer", "integer4",
                        "integer8"                                   -> INT
                        "real", "double_precision"                   -> FLOAT
                        "numeric"                                    -> DECIMAL
                        "character", "character_varying"             -> STRING
                        "tuple"                                      -> STRUCT
                        else                                         -> throw IllegalStateException("No ExprValueType handler for $it")
                    }
                }

                Pair(it, type)
            }.toTypedArray()
        )


        fun fromTypeName(name: String): ExprValueType = LEX_TYPE_MAP[name]
                                                        ?: throw EvaluationException(
                                                            "No such value type for $name",
                                                            ErrorCode.LEXER_INVALID_NAME,
                                                            internal = true)

        fun fromSqlDataType(sqlDataType: PartiqlAst.Type) = fromSqlDataTypeOrNull(sqlDataType)
            ?: throw EvaluationException(
                "No such ExprValueType for ${sqlDataType.javaClass.name}",
                ErrorCode.SEMANTIC_UNION_TYPE_INVALID,
                internal = true
            )

        fun fromSqlDataTypeOrNull(sqlDataType: PartiqlAst.Type) = when (sqlDataType) {
            is BooleanType -> BOOL
            is MissingType -> MISSING
            is NullType -> NULL
            is SmallintType -> INT
            is Integer4Type -> INT
            is Integer8Type -> INT
            is IntegerType -> INT
            is FloatType -> FLOAT
            is RealType -> FLOAT
            is DoublePrecisionType -> FLOAT
            is DecimalType -> DECIMAL
            is NumericType -> DECIMAL
            is TimestampType -> TIMESTAMP
            is CharacterType -> STRING
            is CharacterVaryingType -> STRING
            is StringType -> STRING
            is SymbolType -> SYMBOL
            is ClobType -> CLOB
            is BlobType -> BLOB
            is StructType -> STRUCT
            is TupleType -> STRUCT
            is ListType -> LIST
            is SexpType -> SEXP
            is BagType -> BAG
            is AnyType -> null
            is DateType -> DATE
            is TimeType,
            is TimeWithTimeZoneType -> TIME
            is CustomType -> null
            // TODO: Remove these hardcoded nodes from the PIG domain once [https://github.com/partiql/partiql-lang-kotlin/issues/510] is resolved.
            is EsBoolean,
            is EsInteger,
            is EsText,
            is EsAny,
            is EsFloat,
            is RsBigint,
            is RsBoolean,
            is RsDoublePrecision,
            is RsInteger,
            is RsReal,
            is RsVarcharMax,
            is SparkBoolean,
            is SparkDouble,
            is SparkFloat,
            is SparkInteger,
            is SparkLong,
            is SparkShort -> error("$this node should not be present in PartiQLAST. Consider transforming the AST using CustomTypeVisitorTransform.")
        }
    }
}
