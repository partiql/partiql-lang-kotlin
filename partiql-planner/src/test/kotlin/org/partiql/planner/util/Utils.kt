package org.partiql.planner.util

import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.PType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType

fun <T> cartesianProduct(a: Collection<T>, b: Collection<T>, vararg lists: Collection<T>): Set<List<T>> =
    (listOf(a, b).plus(lists))
        .fold(listOf(listOf<T>())) { acc, set ->
            acc.flatMap { list -> set.map { element -> list + element } }
        }.toSet()

val allSupportedType = StaticType.ALL_TYPES.filterNot {
    it == StaticType.GRAPH || it is NullType || it is MissingType
}

val allDatePType = setOf(PType.date())

val allTimePType = setOf(
    PType.time(6), // TODO: Precision
    PType.timez(6), // TODO: Precision
)

val allTimeStampPType = setOf(
    PType.timestamp(6), // TODO: Precision
    PType.timestampz(6), // TODO: Precision
)

val allDateTimePType = allDatePType + allTimePType + allTimeStampPType

val allCharStringPType = setOf(
    PType.character(256), // TODO: Length
    PType.varchar(256), // TODO: Length
    PType.string(),
    PType.clob(Int.MAX_VALUE), // TODO: Length
)

val allBinaryPType = setOf(
    PType.blob(Int.MAX_VALUE), // TODO: Length
)

val allStructPType = setOf(
    PType.struct(),
    PType.row(), // TODO
)

val allCollectionPType = setOf(
    PType.array(),
    PType.bag()
)

val allBooleanPType = setOf(
    PType.bool()
)

val allIntPType = setOf(PType.tinyint(), PType.smallint(), PType.integer(), PType.bigint(), PType.numeric())

val allNumberPType = allIntPType + setOf(
    PType.decimal(),
    PType.real(),
    PType.doublePrecision(),
)

val allSupportedPType = allNumberPType + allBooleanPType + allCharStringPType + allCollectionPType + allStructPType + allBinaryPType + allDateTimePType

val allSupportedTypeNotUnknown = allSupportedType.filterNot { it == StaticType.MISSING || it == StaticType.NULL }

val allCollectionType = listOf(StaticType.LIST, StaticType.BAG, StaticType.SEXP)

val allTextType = listOf(StaticType.SYMBOL, StaticType.STRING, StaticType.CLOB)

val allDateTimeType = listOf(StaticType.TIME, StaticType.TIMESTAMP, StaticType.DATE)

val allNumberType = StaticType.NUMERIC.allTypes

val allIntType = listOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT)

enum class CastType {
    COERCION, // lossless
    EXPLICIT, // lossy
    UNSAFE // fail
}

val castTable: ((StaticType, StaticType) -> CastType) = { from, to ->
    when (from) {
        is AnyOfType -> CastType.UNSAFE
        is AnyType ->
            when (to) {
                is AnyType -> CastType.COERCION
                else -> CastType.UNSAFE
            }
        is BlobType ->
            when (to) {
                is BlobType -> CastType.COERCION
                else -> CastType.UNSAFE
            }
        is BoolType ->
            when (to) {
                is BoolType, is DecimalType, is FloatType, is IntType -> CastType.COERCION
                is StringType, is SymbolType -> CastType.COERCION
                else -> CastType.UNSAFE
            }
        is ClobType ->
            when (to) {
                is ClobType -> CastType.COERCION
                else -> CastType.UNSAFE
            }
        is BagType -> when (to) {
            is BagType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is ListType -> when (to) {
            is BagType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is SexpType -> when (to) {
            is BagType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is DateType -> when (to) {
            is BagType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is DecimalType -> {
            when (val fromPrecisionScaleConstraint = from.precisionScaleConstraint) {
                is DecimalType.PrecisionScaleConstraint.Unconstrained -> {
                    when (to) {
                        is DecimalType -> {
                            when (to.precisionScaleConstraint) {
                                // to arbitrary precision decimal
                                is DecimalType.PrecisionScaleConstraint.Unconstrained -> CastType.COERCION
                                // to fixed precision decimal
                                is DecimalType.PrecisionScaleConstraint.Constrained -> CastType.EXPLICIT
                            }
                        }
                        is FloatType, is IntType -> CastType.EXPLICIT
                        else -> CastType.UNSAFE
                    }
                }
                is DecimalType.PrecisionScaleConstraint.Constrained -> {
                    // from fixed precision decimal
                    when (to) {
                        is DecimalType -> {
                            when (val toPrecisionScaleConstraint = to.precisionScaleConstraint) {
                                is DecimalType.PrecisionScaleConstraint.Unconstrained -> CastType.COERCION
                                is DecimalType.PrecisionScaleConstraint.Constrained -> {
                                    val toPrecision = toPrecisionScaleConstraint.precision
                                    val toScale = toPrecisionScaleConstraint.scale
                                    val fromPrecision = fromPrecisionScaleConstraint.precision
                                    val fromScale = fromPrecisionScaleConstraint.scale
                                    if (fromPrecision >= toPrecision && fromScale >= toScale) {
                                        CastType.COERCION
                                    } else CastType.EXPLICIT
                                }
                            }
                        }
                        is FloatType -> CastType.COERCION
                        is IntType -> CastType.EXPLICIT
                        else -> CastType.UNSAFE
                    }
                }
            }
        }
        is FloatType -> when (to) {
            is DecimalType -> CastType.COERCION
            is FloatType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is GraphType -> when (to) {
            is GraphType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is IntType -> {
            when (to) {
                is IntType -> {
                    when (from.rangeConstraint) {
                        IntType.IntRangeConstraint.SHORT -> {
                            when (to.rangeConstraint) {
                                IntType.IntRangeConstraint.SHORT -> CastType.COERCION
                                IntType.IntRangeConstraint.INT4 -> CastType.COERCION
                                IntType.IntRangeConstraint.LONG -> CastType.COERCION
                                IntType.IntRangeConstraint.UNCONSTRAINED -> CastType.COERCION
                            }
                        }
                        IntType.IntRangeConstraint.INT4 -> {
                            when (to.rangeConstraint) {
                                IntType.IntRangeConstraint.SHORT -> CastType.UNSAFE
                                IntType.IntRangeConstraint.INT4 -> CastType.COERCION
                                IntType.IntRangeConstraint.LONG -> CastType.COERCION
                                IntType.IntRangeConstraint.UNCONSTRAINED -> CastType.COERCION
                            }
                        }
                        IntType.IntRangeConstraint.LONG -> {
                            when (to.rangeConstraint) {
                                IntType.IntRangeConstraint.SHORT -> CastType.UNSAFE
                                IntType.IntRangeConstraint.INT4 -> CastType.UNSAFE
                                IntType.IntRangeConstraint.LONG -> CastType.COERCION
                                IntType.IntRangeConstraint.UNCONSTRAINED -> CastType.COERCION
                            }
                        }
                        IntType.IntRangeConstraint.UNCONSTRAINED -> {
                            when (to.rangeConstraint) {
                                IntType.IntRangeConstraint.SHORT -> CastType.UNSAFE
                                IntType.IntRangeConstraint.INT4 -> CastType.UNSAFE
                                IntType.IntRangeConstraint.LONG -> CastType.UNSAFE
                                IntType.IntRangeConstraint.UNCONSTRAINED -> CastType.COERCION
                            }
                        }
                    }
                }
                is FloatType -> CastType.COERCION
                is DecimalType -> CastType.COERCION
                else -> CastType.UNSAFE
            }
        }
        MissingType -> when (to) {
            is MissingType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is NullType -> {
            when (to) {
                is MissingType -> CastType.UNSAFE
                else -> CastType.COERCION
            }
        }
        is StringType ->
            when (to) {
                is StringType -> CastType.COERCION
                is SymbolType -> CastType.EXPLICIT
                is ClobType -> CastType.COERCION
                else -> CastType.UNSAFE
            }
        is StructType -> when (to) {
            is StructType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is SymbolType -> when (to) {
            is SymbolType -> CastType.COERCION
            is StringType -> CastType.COERCION
            is ClobType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is TimeType -> when (to) {
            is TimeType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        is TimestampType -> when (to) {
            is TimestampType -> CastType.COERCION
            else -> CastType.UNSAFE
        }
    }
}

val castTablePType: ((PType, PType) -> CastType) = { from, to ->
//    val table = CastTable.partiql
//    when (val result = table.get(from, to)) {
//        null -> CastType.UNSAFE
//        else -> when (result.safety) {
//            Ref.Cast.Safety.UNSAFE -> CastType.UNSAFE
//            Ref.Cast.Safety.COERCION -> CastType.COERCION
//            Ref.Cast.Safety.EXPLICIT -> CastType.EXPLICIT
//        }
//    }
    val fromKind = from.kind
    when (fromKind) {
        PType.Kind.DYNAMIC -> CastType.UNSAFE
        PType.Kind.BLOB -> when (to.kind) {
            PType.Kind.BLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.BOOL -> when (to.kind) {
            PType.Kind.BOOL, PType.Kind.DECIMAL, PType.Kind.REAL, PType.Kind.DOUBLE, PType.Kind.INTEGER, PType.Kind.TINYINT, PType.Kind.SMALLINT, PType.Kind.BIGINT, PType.Kind.NUMERIC -> CastType.COERCION
            PType.Kind.STRING -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.CLOB -> when (to.kind) {
            PType.Kind.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.BAG -> when (to.kind) {
            PType.Kind.BAG -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.ARRAY -> when (to.kind) {
            PType.Kind.BAG -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.DATE -> when (to.kind) {
            PType.Kind.DATE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.DECIMAL -> {
            when (val toKind = to.kind) {
                PType.Kind.DECIMAL -> {
                    val toPrecision = to.precision
                    val toScale = to.scale
                    val fromPrecision = from.precision
                    val fromScale = from.scale
                    if (fromPrecision >= toPrecision && fromScale >= toScale) {
                        CastType.COERCION
                    } else CastType.EXPLICIT
                }
                PType.Kind.REAL -> CastType.COERCION
                PType.Kind.DOUBLE -> CastType.COERCION
                PType.Kind.INTEGER -> CastType.EXPLICIT
                else -> CastType.UNSAFE
            }
        }
        PType.Kind.REAL -> when (to.kind) {
            PType.Kind.REAL -> CastType.COERCION
            PType.Kind.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.TINYINT -> when (to.kind) {
            PType.Kind.TINYINT, PType.Kind.SMALLINT, PType.Kind.INTEGER, PType.Kind.BIGINT, PType.Kind.NUMERIC, PType.Kind.DECIMAL, PType.Kind.REAL, PType.Kind.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.SMALLINT -> when (to.kind) {
            PType.Kind.SMALLINT, PType.Kind.INTEGER, PType.Kind.BIGINT, PType.Kind.NUMERIC, PType.Kind.DECIMAL, PType.Kind.REAL, PType.Kind.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.INTEGER -> when (to.kind) {
            PType.Kind.INTEGER, PType.Kind.BIGINT, PType.Kind.NUMERIC, PType.Kind.DECIMAL, PType.Kind.REAL, PType.Kind.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.BIGINT -> when (to.kind) {
            PType.Kind.BIGINT, PType.Kind.NUMERIC, PType.Kind.DECIMAL, PType.Kind.REAL, PType.Kind.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.STRING -> when (to.kind) {
            PType.Kind.STRING, PType.Kind.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.STRUCT -> when (to.kind) {
            PType.Kind.STRUCT -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.TIME, PType.Kind.TIMEZ -> when (to.kind) {
            PType.Kind.TIME, PType.Kind.TIMEZ -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.TIMESTAMP, PType.Kind.TIMESTAMPZ -> when (to.kind) {
            PType.Kind.TIMESTAMP, PType.Kind.TIMESTAMPZ -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.NUMERIC -> when (to.kind) {
            PType.Kind.NUMERIC, PType.Kind.DECIMAL, PType.Kind.REAL, PType.Kind.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.DOUBLE -> when (to.kind) {
            PType.Kind.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.CHAR -> when (to.kind) {
            PType.Kind.CHAR, PType.Kind.VARCHAR, PType.Kind.STRING, PType.Kind.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.VARCHAR -> when (to.kind) {
            PType.Kind.VARCHAR, PType.Kind.STRING, PType.Kind.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.ROW -> when (to.kind) {
            PType.Kind.ROW, PType.Kind.STRUCT -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.Kind.UNKNOWN -> CastType.UNSAFE
        PType.Kind.VARIANT -> TODO()
    }
}
