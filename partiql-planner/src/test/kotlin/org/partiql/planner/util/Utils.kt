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
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType

fun <T> cartesianProduct(a: List<T>, b: List<T>, vararg lists: List<T>): Set<List<T>> =
    (listOf(a, b).plus(lists))
        .fold(listOf(listOf<T>())) { acc, set ->
            acc.flatMap { list -> set.map { element -> list + element } }
        }.toSet()

val allSupportedType = StaticType.ALL_TYPES.filterNot { it == StaticType.GRAPH }

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
        is NullType -> when (to) {
            is NullType -> CastType.COERCION
            else -> CastType.UNSAFE
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
