/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.partiql.value

/**
 * PartiQL Type Names
 */
@PartiQLValueExperimental
public enum class PartiQLValueType {
    ANY,
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,
    INT,
    // For now, just distinguish between fixed precision and arbitrary precision
    NUMERIC, // TODO: Simple enum class does not have the power to express the parameterized type
    NUMERIC_ARBITRARY, // arbitrary precision decimal
    FLOAT32,
    FLOAT64,
    CHAR,
    STRING,
    SYMBOL,
    BINARY,
    BYTE,
    BLOB,
    CLOB,
    DATE,
    TIME,
    TIMESTAMP,
    INTERVAL,
    BAG,
    LIST,
    SEXP,
    STRUCT,
    NULL,
    MISSING,
}

public sealed interface PartiQLType {
    public sealed interface Runtime : PartiQLType {

        public object MissingType : Runtime

        public sealed interface Core : Runtime

        /**
         * This is just to show what we might want to use this for.
         */
        private interface Custom : Runtime
    }

    public sealed interface Abstract : PartiQLType {
        /**
         * Refers to the top-level type.
         */
        public object Any
    }
}

public object AnyType : PartiQLType.Abstract

public data class BitType(
    val length: Int
) : PartiQLType.Runtime.Core

public data class BitVaryingType(
    val length: Int
) : PartiQLType.Runtime.Core

/**
 * This is SQL:1999's BINARY LARGE OBJECT and Ion's BLOB type
 *
 * Aliases included BLOB
 */
public data class BlobType(
    val length: Int
) : PartiQLType.Runtime.Core {
    public companion object {
        @JvmStatic
        public val MAXIMUM_LENGTH: Int = 2_147_483_647 // TODO: Define MAXIMUM. Here is Oracle's
    }
}

public object BoolType : PartiQLType.Runtime.Core

/**
 * TODO: Should this be allowed? It's not in SQL:1999
 */
public object ByteType : PartiQLType.Runtime.Core

/**
 * SQL:1999's CHARACTER type
 */
public data class CharType(
    val length: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's CHARACTER VARYING(n) type
 * Aliases are VARCHAR(n), STRING(n), and SYMBOL(n)
 */
public data class CharVarType(
    val length: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's CHARACTER VARYING type
 * Aliases are VARCHAR, STRING, and SYMBOL (both are unbounded in length)
 */
public object CharVarUnboundedType : PartiQLType.Runtime.Core

/**
 * SQL:1999's CHARACTER LARGE OBJECT(n) type and Ion's CLOB type
 * Aliases are CLOB(n)
 */
public data class ClobType(
    val length: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's CHARACTER LARGE OBJECT type and Ion's CLOB type
 * Aliases are CLOB
 */
public data class ClobUnboundedType(
    val length: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's DATE type
 * TODO: Does this differ from Ion?
 */
public object DateType : PartiQLType.Runtime.Core

public object Int8Type : PartiQLType.Runtime.Core

public object Int16Type : PartiQLType.Runtime.Core

public object Int32Type : PartiQLType.Runtime.Core

public object Int64Type : PartiQLType.Runtime.Core

/**
 * Approximate Numeric Type
 *
 * Aliases include: REAL
 */
public object Float32Type : PartiQLType.Runtime.Core

/**
 * Approximate Numeric Type
 *
 * Aliases include: DOUBLE PRECISION
 * TODO: What is SQL:1999's `FLOAT`?
 */
public object Float64Type : PartiQLType.Runtime.Core

/**
 * Aliases include DECIMAL(p, s)
 */
public data class NumericType(
    val precision: Int?,
    val scale: Int?
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's TIME WITHOUT TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimeType(
    val precision: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's TIME WITH TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimeWithTimeZoneType(
    val precision: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's TIMESTAMP WITHOUT TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimestampType(
    val precision: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's TIMESTAMP WITH TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimestampWithTimeZoneType(
    val precision: Int
) : PartiQLType.Runtime.Core

/**
 * SQL:1999's INTERVAL type
 */
public data class IntervalType(
    // TODO: Does this need a `fields` property?
    val precision: Int
) : PartiQLType.Runtime.Core

/**
 * PartiQL's BAG type
 */
public data class BagType(
    val element: PartiQLType
) : PartiQLType.Runtime.Core

/**
 * PartiQL's Array type
 *
 * Aliases include LIST
 */
public data class ArrayType(
    val element: PartiQLType
) : PartiQLType.Runtime.Core

/**
 * PartiQL's Tuple type
 *
 * Aliases include STRUCT TODO: Are we sure?
 */
public data class TupleType(
    val fields: PartiQLType
) : PartiQLType.Runtime.Core

/**
 * Ion's NULL.NULL type
 */
public object NullType : PartiQLType.Runtime.Core
