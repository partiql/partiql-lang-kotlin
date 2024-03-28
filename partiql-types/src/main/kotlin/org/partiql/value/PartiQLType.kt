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

public sealed interface PartiQLType {
    public val name: String

    public sealed interface Runtime : PartiQLType {

        public sealed interface Core : Runtime

        /**
         * This is just to show what we might want to use this for.
         */
        public interface Custom : Runtime
    }

    public sealed interface Abstract : PartiQLType {
        /**
         * Refers to the top-level type.
         * NOTE: THIS ACTUALLY ISN'T USED YET. SEE THE DYNAMIC TYPE.
         */
        public object Any
    }

    public companion object {

        @OptIn(PartiQLValueExperimental::class)
        @Deprecated("Should not be used")
        public fun fromLegacy(type: PartiQLValueType): PartiQLType = when (type) {
            PartiQLValueType.ANY -> DynamicType
            PartiQLValueType.BOOL -> BoolType
            PartiQLValueType.INT8 -> Int8Type
            PartiQLValueType.INT16 -> Int16Type
            PartiQLValueType.INT32 -> Int32Type
            PartiQLValueType.INT64 -> Int64Type
            PartiQLValueType.INT -> TypeIntBig
            PartiQLValueType.DECIMAL -> TypeNumericUnbounded
            PartiQLValueType.DECIMAL_ARBITRARY -> TypeNumericUnbounded
            PartiQLValueType.FLOAT32 -> TypeReal
            PartiQLValueType.FLOAT64 -> TypeDoublePrecision
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
                    is DecimalType.PrecisionScaleConstraint.Unconstrained -> TypeNumericUnbounded
                    is DecimalType.PrecisionScaleConstraint.Constrained -> NumericType(
                        constraint.precision,
                        constraint.scale
                    )
                }
            }
            is FloatType -> TypeDoublePrecision // TODO: What about Float 32?
            is GraphType -> TODO()
            is IntType -> when (type.rangeConstraint) {
                IntType.IntRangeConstraint.SHORT -> Int16Type
                IntType.IntRangeConstraint.INT4 -> Int32Type
                IntType.IntRangeConstraint.LONG -> Int64Type
                IntType.IntRangeConstraint.UNCONSTRAINED -> TypeIntBig
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
            repeat(NumericType.MAX_PRECISION + 1) { precision ->
                repeat(precision + 1) { scale ->
                    add(NumericType(precision, scale))
                }
            }
        }

        // TODO: I'm pretty sure this is wrong, but I'll just publish this for now
        @JvmStatic
        @Deprecated("Will likely be removed")
        public val APPROXIMATE_NUMERIC_TYPES: List<PartiQLType> = buildList {
            add(TypeReal)
            add(TypeDoublePrecision)
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val CHAR_TYPES: List<PartiQLType> = buildList {
            repeat(CharType.MAX_LENGTH + 1) { length ->
                add(CharType(length))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val VARCHAR_TYPES: List<PartiQLType> = buildList {
            repeat(CharVarType.MAX_LENGTH + 1) { length ->
                add(CharVarType(length))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val CLOB_TYPES: List<PartiQLType> = buildList {
            repeat(ClobType.MAX_LENGTH + 1) { length ->
                add(ClobType(length))
            }
            add(ClobUnboundedType)
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val BLOB_TYPES: List<PartiQLType> = buildList {
            repeat(BlobType.MAXIMUM_LENGTH + 1) { length ->
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
            repeat(TimeType.MAX_PRECISION + 1) { precision ->
                add(TimeType(precision))
            }
            repeat(TimeWithTimeZoneType.MAX_PRECISION + 1) { precision ->
                add(TimeWithTimeZoneType(precision))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val TIMESTAMP_TYPES: List<PartiQLType> = buildList {
            repeat(TimestampType.MAX_PRECISION + 1) { precision ->
                add(TimestampType(precision))
            }
            repeat(TimestampWithTimeZoneType.MAX_PRECISION + 1) { precision ->
                add(TimestampWithTimeZoneType(precision))
            }
        }

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val INTERVAL_TYPES: List<PartiQLType> = buildList {
            repeat(IntervalType.MAX_PRECISION + 1) { precision ->
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
            Int64Type,
            TypeIntBig
        ) +
            NUMERIC_BOUND_TYPES +
            APPROXIMATE_NUMERIC_TYPES +
            listOf(NumericType.UNCONSTRAINED) +
            TEXT_TYPES +
            BINARY_TYPES +
            DATETIME_TYPES +
            COLLECTION_TYPES +
            COMPLEX_TYPES +
            listOf(DynamicType)

        @JvmStatic
        @Deprecated("Will likely be removed")
        public val PRECEDENCE_MAP: Map<PartiQLType, Int> = ALL_TYPES.mapIndexed { precedence, type -> type to precedence }.toMap()
    }
}
