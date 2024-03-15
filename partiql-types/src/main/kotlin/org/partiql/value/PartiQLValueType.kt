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

import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.SingleType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType

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
            PartiQLValueType.NUMERIC -> NumericType(null, null) // TODO: Set Max?
            PartiQLValueType.NUMERIC_ARBITRARY -> NumericType(null, null)
            PartiQLValueType.FLOAT32 -> Float32Type
            PartiQLValueType.FLOAT64 -> Float64Type
            PartiQLValueType.CHAR -> CharType(CharType.MAX_LENGTH)
            PartiQLValueType.STRING -> CharVarUnboundedType
            PartiQLValueType.SYMBOL -> CharVarUnboundedType
            PartiQLValueType.BINARY -> BlobType(BlobType.MAXIMUM_LENGTH)
            PartiQLValueType.BYTE -> ByteType
            PartiQLValueType.BLOB -> BlobType(BlobType.MAXIMUM_LENGTH)
            PartiQLValueType.CLOB -> ClobType(ClobType.MAX_LENGTH)
            PartiQLValueType.DATE -> DateType
            PartiQLValueType.TIME -> TimeType(TimeType.MAX_PRECISION)
            PartiQLValueType.TIMESTAMP -> TimestampType(TimestampType.MAX_PRECISION)
            PartiQLValueType.INTERVAL -> IntervalType(IntervalType.MAX_PRECISION)
            PartiQLValueType.BAG -> BagType
            PartiQLValueType.LIST -> ArrayType
            PartiQLValueType.SEXP -> ArrayType
            PartiQLValueType.STRUCT -> TupleType
            PartiQLValueType.NULL -> NullType
            PartiQLValueType.MISSING -> MissingType
        }

        @Deprecated("Should not be used")
        public fun fromSingleType(type: SingleType): PartiQLType = when (type) {
            is org.partiql.types.NullType -> NullType
            is org.partiql.types.BlobType -> BlobType(BlobType.MAXIMUM_LENGTH) // TODO
            is org.partiql.types.BoolType -> BoolType
            is org.partiql.types.ClobType -> ClobType(ClobType.MAX_LENGTH)
            is org.partiql.types.BagType -> BagType
            is ListType -> ArrayType
            is SexpType -> ArrayType
            is org.partiql.types.DateType -> DateType
            is DecimalType -> {
                when (val constraint = type.precisionScaleConstraint) {
                    is DecimalType.PrecisionScaleConstraint.Unconstrained -> NumericType(null, null)
                    is DecimalType.PrecisionScaleConstraint.Constrained -> NumericType(constraint.precision, constraint.scale)
                }
            }
            is FloatType -> Float64Type // TODO: What about Float 32?
            is GraphType -> TODO()
            is IntType -> when (type.rangeConstraint) {
                IntType.IntRangeConstraint.SHORT -> Int16Type
                IntType.IntRangeConstraint.INT4 -> Int32Type
                IntType.IntRangeConstraint.LONG -> Int64Type
                IntType.IntRangeConstraint.UNCONSTRAINED -> NumericType(null, 0)
            }
            org.partiql.types.MissingType -> MissingType
            is StringType -> CharVarUnboundedType
            is StructType -> TupleType
            is SymbolType -> CharVarUnboundedType
            is org.partiql.types.TimeType -> TimeType(TimeType.MAX_PRECISION)
            is org.partiql.types.TimestampType -> TimestampType(TimestampType.MAX_PRECISION)
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
            add(ArrayType)
            add(BagType)
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val COMPLEX_TYPES: List<PartiQLType> = buildList {
            add(TupleType)
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
            listOf(NumericType(null, 0)) + // Unbound INT
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

@Deprecated("How can I make this internal?")
public abstract class PartiQLCoreTypeBase : PartiQLType.Runtime.Core {
    override fun toString(): String = this.name
}

public object AnyType : PartiQLType.Abstract {
    override val name: String = "ANY"
    override fun toString(): String = this.name
}

public data class BitType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "BIT"
    override fun toString(): String = "${this.name}(${this.length})"
}

public data class BitVaryingType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "BIT_VARYING"
    override fun toString(): String = "${this.name}(${this.length})"
}

/**
 * This is SQL:1999's BINARY LARGE OBJECT and Ion's BLOB type
 *
 * Aliases included BLOB
 */
public data class BlobType(
    val length: Int
) : PartiQLCoreTypeBase() {

    override val name: String = "BLOB"
    public companion object {
        @JvmStatic
        public val MAXIMUM_LENGTH: Int = 10 // TODO: Define MAXIMUM. Here is Oracle's: 2_147_483_647
    }

    override fun toString(): String = "${this.name}(${this.length})"
}

public object BoolType : PartiQLCoreTypeBase() {
    public override val name: String = "BOOL"
}

/**
 * TODO: Should this be allowed? It's not in SQL:1999
 */
public object ByteType : PartiQLCoreTypeBase() {
    override val name: String = "BYTE"
}

/**
 * SQL:1999's CHARACTER type
 */
public data class CharType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "CHAR"
    override fun toString(): String = "${this.name}(${this.length})"
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
) : PartiQLCoreTypeBase() {
    override val name: String = "VARCHAR" // TODO: For now
    override fun toString(): String = "${this.name}(${this.length})"

    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}

/**
 * SQL:1999's CHARACTER VARYING type
 * Aliases are VARCHAR, STRING, and SYMBOL (both are unbounded in length)
 */
public object CharVarUnboundedType : PartiQLCoreTypeBase() {
    override val name: String = "STRING" // TODO: For now
}

/**
 * SQL:1999's CHARACTER LARGE OBJECT(n) type and Ion's CLOB type
 * Aliases are CLOB(n)
 */
public data class ClobType(
    val length: Int
) : PartiQLCoreTypeBase() {
    override val name: String = "CLOB"
    override fun toString(): String = "${this.name}(${this.length})"

    public companion object {
        public const val MAX_LENGTH: Int = 250 // TODO
    }
}

/**
 * SQL:1999's CHARACTER LARGE OBJECT type and Ion's CLOB type
 * Aliases are CLOB
 */
public object ClobUnboundedType : PartiQLCoreTypeBase() {
    override val name: String = "CLOB"
}

/**
 * SQL:1999's DATE type
 * TODO: Does this differ from Ion?
 */
public object DateType : PartiQLCoreTypeBase() {
    override val name: String = "DATE"
}

public object Int8Type : PartiQLCoreTypeBase() {
    override val name: String = "INT8"
}

public object Int16Type : PartiQLCoreTypeBase() {
    override val name: String = "INT16"
}

public object Int32Type : PartiQLCoreTypeBase() {
    override val name: String = "INT32"
}

public object Int64Type : PartiQLCoreTypeBase() {
    override val name: String = "INT64"
}

/**
 * Approximate Numeric Type
 *
 * Aliases include: REAL
 */
public object Float32Type : PartiQLCoreTypeBase() {
    override val name: String = "FLOAT32"
}

/**
 * Approximate Numeric Type
 *
 * Aliases include: DOUBLE PRECISION
 * TODO: What is SQL:1999's `FLOAT`?
 */
public object Float64Type : PartiQLCoreTypeBase() {
    override val name: String = "FLOAT64"
}

/**
 * Aliases include DECIMAL(p, s)
 */
public data class NumericType(
    val precision: Int?,
    val scale: Int?
) : PartiQLCoreTypeBase() {
    override val name: String = "NUMERIC"
    override fun toString(): String = "${this.name}(${this.precision}, ${this.scale})"

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
) : PartiQLCoreTypeBase() {
    override val name: String = "TIME"
    override fun toString(): String = "${this.name}(${this.precision})"

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
) : PartiQLCoreTypeBase() {
    override val name: String = "TIME_WITH_TIME_ZONE"
    override fun toString(): String = "${this.name}(${this.precision})"

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
) : PartiQLCoreTypeBase() {
    override val name: String = "TIMESTAMP"
    override fun toString(): String = "${this.name}(${this.precision})"

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
) : PartiQLCoreTypeBase() {
    override val name: String = "TIMESTAMP_WITH_TIME_ZONE"
    override fun toString(): String = "${this.name}(${this.precision})"

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
) : PartiQLCoreTypeBase() {
    override val name: String = "INTERVAL"
    override fun toString(): String = "${this.name}(${this.precision})"

    public companion object {
        @JvmStatic
        public val MAX_PRECISION: Int = 10 // TODO: Actually do
    }
}

/**
 * PartiQL's BAG type
 */
public object BagType : PartiQLCoreTypeBase() {
    override val name: String = "BAG"
    override fun toString(): String = this.name
}

/**
 * PartiQL's Array type
 *
 * Aliases include LIST
 */
public object ArrayType : PartiQLCoreTypeBase() {
    override val name: String = "ARRAY"
    override fun toString(): String = this.name
}

/**
 * PartiQL's Tuple type
 *
 * Aliases include STRUCT TODO: Are we sure?
 */
public object TupleType : PartiQLCoreTypeBase() {
    override val name: String = "TUPLE"
    override fun toString(): String = this.name
}

/**
 * Ion's NULL.NULL type
 */
public object NullType : PartiQLCoreTypeBase() {
    override val name: String = "NULL"
}
