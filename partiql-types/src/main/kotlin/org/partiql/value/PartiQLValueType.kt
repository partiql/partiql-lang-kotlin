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
    public val name: String

    public sealed interface Runtime : PartiQLType {

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

    public companion object {

        @OptIn(PartiQLValueExperimental::class)
        @Deprecated("Should not be used")
        public fun fromLegacy(type: PartiQLValueType): PartiQLType = when (type) {
            PartiQLValueType.ANY -> AnyType
            PartiQLValueType.BOOL -> BoolType
            PartiQLValueType.INT8 -> Int8Type
            PartiQLValueType.INT16 -> Int16Type
            PartiQLValueType.INT32 -> Int32Type
            PartiQLValueType.INT64 -> Int64Type
            PartiQLValueType.INT -> NumericType(null, 0)
            PartiQLValueType.NUMERIC -> NumericType(null, null) // TODO: What are the bounds to this?
            PartiQLValueType.NUMERIC_ARBITRARY -> NumericType(null, null)
            PartiQLValueType.FLOAT32 -> Float32Type
            PartiQLValueType.FLOAT64 -> Float64Type
            PartiQLValueType.CHAR -> CharType(1) // TODO: What to do here?
            PartiQLValueType.STRING -> CharVarUnboundedType
            PartiQLValueType.SYMBOL -> CharVarUnboundedType
            PartiQLValueType.BINARY -> BlobType(10) // TODO: What to do here?
            PartiQLValueType.BYTE -> ByteType
            PartiQLValueType.BLOB -> BlobType(10) // TODO: What to do here?
            PartiQLValueType.CLOB -> ClobType(10) // TODO what to do here?
            PartiQLValueType.DATE -> DateType
            PartiQLValueType.TIME -> TimeType(10) // TODO: Precision?
            PartiQLValueType.TIMESTAMP -> TimestampType(10) // TODO: Precision?
            PartiQLValueType.INTERVAL -> IntervalType(10) // TODO: Precision?
            PartiQLValueType.BAG -> BagType(AnyType)
            PartiQLValueType.LIST -> ArrayType(AnyType)
            PartiQLValueType.SEXP -> ArrayType(AnyType)
            PartiQLValueType.STRUCT -> TupleType(AnyType)
            PartiQLValueType.NULL -> NullType
            PartiQLValueType.MISSING -> MissingType
        }

        // TODO: I'm pretty sure this is wrong, but I'll just publish this for now
        @JvmStatic
        @Deprecated("Will likely be removed")
        public val NUMERIC_BOUND_TYPES: List<NumericType> = buildList {
            repeat(NumericType.MAX_PRECISION) { precision ->
                repeat(NumericType.MAX_SCALE) { scale ->
                    add(NumericType(precision, scale))
                }
            }
            add(NumericType.UNCONSTRAINED)
        }

        // TODO: I'm pretty sure this is wrong, but I'll just publish this for now
        @JvmStatic
        @Deprecated("Will likely be removed")
        public val APPROXIMATE_NUMERIC_TYPES: List<PartiQLType> = buildList {
            add(Float32Type)
            add(Float64Type)
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val CHAR_TYPES: List<PartiQLType> = buildList {
            repeat(CharType.MAX_LENGTH) { length ->
                add(CharType(length))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val VARCHAR_TYPES: List<PartiQLType> = buildList {
            repeat(CharVarType.MAX_LENGTH) { length ->
                add(CharVarType(length))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val CLOB_TYPES: List<PartiQLType> = buildList {
            repeat(ClobType.MAX_LENGTH) { length ->
                add(ClobType(length))
            }
            add(ClobUnboundedType)
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val BLOB_TYPES: List<PartiQLType> = buildList {
            repeat(BlobType.MAXIMUM_LENGTH) { length ->
                add(BlobType(length))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val BINARY_TYPES: List<PartiQLType> = buildList {
            // TODO: Does BinaryType exist?
            add(ByteType)
            addAll(BLOB_TYPES)
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val TIME_TYPES: List<PartiQLType> = buildList {
            repeat(TimeType.MAX_PRECISION) { precision ->
                add(TimeType(precision))
            }
            repeat(TimeWithTimeZoneType.MAX_PRECISION) { precision ->
                add(TimeWithTimeZoneType(precision))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val TIMESTAMP_TYPES: List<PartiQLType> = buildList {
            repeat(TimestampType.MAX_PRECISION) { precision ->
                add(TimestampType(precision))
            }
            repeat(TimestampWithTimeZoneType.MAX_PRECISION) { precision ->
                add(TimestampWithTimeZoneType(precision))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val INTERVAL_TYPES: List<PartiQLType> = buildList {
            repeat(IntervalType.MAX_PRECISION) { precision ->
                add(IntervalType(precision))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val DATETIME_TYPES: List<PartiQLType> = buildList {
            add(DateType)
            addAll(BLOB_TYPES)
            addAll(TIME_TYPES)
            addAll(TIMESTAMP_TYPES)
            addAll(INTERVAL_TYPES)
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val COLLECTION_TYPES: List<PartiQLType> = buildList {
            add(ArrayType())
            add(BagType())
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val COMPLEX_TYPES: List<PartiQLType> = buildList {
            add(TupleType())
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val TEXT_TYPES: List<PartiQLType> = buildList {
            addAll(CHAR_TYPES)
            addAll(VARCHAR_TYPES)
            add(CharVarUnboundedType)
            addAll(CLOB_TYPES)
        }

        /**
         * This has the precedence -- which is currently unspecified.
         */
        @JvmStatic
        @Deprecated("Will likely be removed")
        public val ALL_TYPES: List<PartiQLType> = listOf(
            NullType,
            MissingType,
            BoolType,
            Int8Type,
            Int16Type,
            Int32Type,
            Int64Type
        ) +
            NUMERIC_BOUND_TYPES +
            APPROXIMATE_NUMERIC_TYPES +
            listOf(NumericType.UNCONSTRAINED) +
            TEXT_TYPES +
            BLOB_TYPES +
            DATETIME_TYPES +
            COLLECTION_TYPES +
            COMPLEX_TYPES +
            listOf(AnyType)

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val PRECEDENCE_MAP: Map<PartiQLType, Int> = ALL_TYPES.mapIndexed { precedence, type -> type to precedence }.toMap()
    }
}

public object AnyType : PartiQLType.Abstract {
    override val name: String = "ANY"
}

public data class BitType(
    val length: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "BIT"
}

public data class BitVaryingType(
    val length: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "BIT_VARYING"
}

/**
 * This is SQL:1999's BINARY LARGE OBJECT and Ion's BLOB type
 *
 * Aliases included BLOB
 */
public data class BlobType(
    val length: Int
) : PartiQLType.Runtime.Core {

    override val name: String = "BLOB"
    public companion object {
        @JvmStatic
        public val MAXIMUM_LENGTH: Int = 2_147_483_647 // TODO: Define MAXIMUM. Here is Oracle's
    }
}

public object BoolType : PartiQLType.Runtime.Core {
    override val name: String = "BOOL"
}

/**
 * TODO: Should this be allowed? It's not in SQL:1999
 */
public object ByteType : PartiQLType.Runtime.Core {
    override val name: String = "BYTE"
}

/**
 * SQL:1999's CHARACTER type
 */
public data class CharType(
    val length: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "CHAR"
    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}

/**
 * SQL:1999's CHARACTER VARYING(n) type
 * Aliases are VARCHAR(n), STRING(n), and SYMBOL(n)
 */
public data class CharVarType(
    val length: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "VARCHAR" // TODO: For now

    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}

/**
 * SQL:1999's CHARACTER VARYING type
 * Aliases are VARCHAR, STRING, and SYMBOL (both are unbounded in length)
 */
public object CharVarUnboundedType : PartiQLType.Runtime.Core {
    override val name: String = "STRING" // TODO: For now
}

/**
 * SQL:1999's CHARACTER LARGE OBJECT(n) type and Ion's CLOB type
 * Aliases are CLOB(n)
 */
public data class ClobType(
    val length: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "CLOB"

    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}

/**
 * SQL:1999's CHARACTER LARGE OBJECT type and Ion's CLOB type
 * Aliases are CLOB
 */
public object ClobUnboundedType : PartiQLType.Runtime.Core {
    override val name: String = "CLOB"
}

/**
 * SQL:1999's DATE type
 * TODO: Does this differ from Ion?
 */
public object DateType : PartiQLType.Runtime.Core {
    override val name: String = "DATE"
}

public object Int8Type : PartiQLType.Runtime.Core {
    override val name: String = "INT8"
}

public object Int16Type : PartiQLType.Runtime.Core {
    override val name: String = "INT16"
}

public object Int32Type : PartiQLType.Runtime.Core {
    override val name: String = "INT32"

}

public object Int64Type : PartiQLType.Runtime.Core {
    override val name: String = "INT64"

}

/**
 * Approximate Numeric Type
 *
 * Aliases include: REAL
 */
public object Float32Type : PartiQLType.Runtime.Core {
    override val name: String = "FLOAT32"
}

/**
 * Approximate Numeric Type
 *
 * Aliases include: DOUBLE PRECISION
 * TODO: What is SQL:1999's `FLOAT`?
 */
public object Float64Type : PartiQLType.Runtime.Core {
    override val name: String = "FLOAT64"
}

/**
 * Aliases include DECIMAL(p, s)
 */
public data class NumericType(
    val precision: Int?,
    val scale: Int?
) : PartiQLType.Runtime.Core {
    override val name: String = "NUMERIC"

    public companion object {
        public const val MAX_PRECISION: Int = 38 // TODO
        public const val MIN_PRECISION: Int = 0 // TODO
        public const val MIN_SCALE: Int = 0 // TODO
        public const val MAX_SCALE: Int = 38 // TODO

        public val UNCONSTRAINED: NumericType = NumericType(null, null)
    }
}

/**
 * SQL:1999's TIME WITHOUT TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimeType(
    val precision: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "TIME"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}

/**
 * SQL:1999's TIME WITH TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimeWithTimeZoneType(
    val precision: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "TIME_WITH_TIME_ZONE"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}

/**
 * SQL:1999's TIMESTAMP WITHOUT TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimestampType(
    val precision: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "TIMESTAMP"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}

/**
 * SQL:1999's TIMESTAMP WITH TIME ZONE type
 * TODO: Does this differ from Ion?
 */
public data class TimestampWithTimeZoneType(
    val precision: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "TIMESTAMP_WITH_TIME_ZONE"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}

/**
 * SQL:1999's INTERVAL type
 */
public data class IntervalType(
    // TODO: Does this need a `fields` property?
    val precision: Int
) : PartiQLType.Runtime.Core {
    override val name: String = "INTERVAL"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}

/**
 * PartiQL's BAG type
 */
public data class BagType(
    val element: PartiQLType = AnyType
) : PartiQLType.Runtime.Core {
    override val name: String = "BAG"
}

/**
 * PartiQL's Array type
 *
 * Aliases include LIST
 */
public data class ArrayType(
    val element: PartiQLType = AnyType
) : PartiQLType.Runtime.Core {
    override val name: String = "ARRAY"
}

/**
 * PartiQL's Tuple type
 *
 * Aliases include STRUCT TODO: Are we sure?
 */
public data class TupleType(
    val fields: PartiQLType = AnyType
) : PartiQLType.Runtime.Core {
    override val name: String = "TUPLE"
}

/**
 * Ion's NULL.NULL type
 */
public object NullType : PartiQLType.Runtime.Core {
    override val name: String = "NULL"
}
