package org.partiql.planner.util

import org.partiql.spi.types.PType
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

val allIntPType = setOf(PType.tinyint(), PType.smallint(), PType.integer(), PType.bigint())

val allNumberPType = allIntPType + setOf(
    PType.decimal(38, 19),
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
    val fromKind = from.code()
    when (fromKind) {
        PType.DYNAMIC -> CastType.UNSAFE
        PType.BLOB -> when (to.code()) {
            PType.BLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.BOOL -> when (to.code()) {
            PType.BOOL, PType.DECIMAL, PType.REAL, PType.DOUBLE, PType.INTEGER, PType.TINYINT, PType.SMALLINT, PType.BIGINT, PType.NUMERIC -> CastType.COERCION
            PType.STRING -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.CLOB -> when (to.code()) {
            PType.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.BAG -> when (to.code()) {
            PType.BAG -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.ARRAY -> when (to.code()) {
            PType.BAG -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.DATE -> when (to.code()) {
            PType.DATE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.DECIMAL -> {
            when (val toKind = to.code()) {
                PType.DECIMAL -> {
                    val toPrecision = to.precision
                    val toScale = to.scale
                    val fromPrecision = from.precision
                    val fromScale = from.scale
                    if (fromPrecision >= toPrecision && fromScale >= toScale) {
                        CastType.COERCION
                    } else CastType.EXPLICIT
                }
                PType.REAL -> CastType.COERCION
                PType.DOUBLE -> CastType.COERCION
                PType.INTEGER -> CastType.EXPLICIT
                else -> CastType.UNSAFE
            }
        }
        PType.REAL -> when (to.code()) {
            PType.REAL -> CastType.COERCION
            PType.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.TINYINT -> when (to.code()) {
            PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT, PType.NUMERIC, PType.DECIMAL, PType.REAL, PType.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.SMALLINT -> when (to.code()) {
            PType.SMALLINT, PType.INTEGER, PType.BIGINT, PType.NUMERIC, PType.DECIMAL, PType.REAL, PType.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.INTEGER -> when (to.code()) {
            PType.INTEGER, PType.BIGINT, PType.NUMERIC, PType.DECIMAL, PType.REAL, PType.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.BIGINT -> when (to.code()) {
            PType.BIGINT, PType.NUMERIC, PType.DECIMAL, PType.REAL, PType.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.STRING -> when (to.code()) {
            PType.STRING, PType.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.STRUCT -> when (to.code()) {
            PType.STRUCT -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.TIME, PType.TIMEZ -> when (to.code()) {
            PType.TIME, PType.TIMEZ -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.TIMESTAMP, PType.TIMESTAMPZ -> when (to.code()) {
            PType.TIMESTAMP, PType.TIMESTAMPZ -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.NUMERIC -> when (to.code()) {
            PType.NUMERIC, PType.DECIMAL, PType.REAL, PType.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.DOUBLE -> when (to.code()) {
            PType.DOUBLE -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.CHAR -> when (to.code()) {
            PType.CHAR, PType.VARCHAR, PType.STRING, PType.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.VARCHAR -> when (to.code()) {
            PType.VARCHAR, PType.STRING, PType.CLOB -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.ROW -> when (to.code()) {
            PType.ROW, PType.STRUCT -> CastType.COERCION
            else -> CastType.UNSAFE
        }
        PType.UNKNOWN -> CastType.UNSAFE
        PType.VARIANT -> TODO()
        else -> {
            error("Unknown type: $fromKind")
        }
    }
}
