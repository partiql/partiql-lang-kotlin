package org.partiql.planner.internal

import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.DecimalType.PrecisionScaleConstraint
import org.partiql.types.Field
import org.partiql.types.FloatType
import org.partiql.types.IntType
import org.partiql.types.IntType.IntRangeConstraint
import org.partiql.types.ListType
import org.partiql.types.PType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint
import java.util.stream.Collectors

/**
 * PType conversion helper functions.
 */
internal object SqlTypes {

    fun fromStaticType(type: StaticType): PType {
        when (type) {
            is AnyType -> {
                return PType.dynamic()
            }
            is AnyOfType -> {
                val allTypes = HashSet(type.flatten().allTypes)
                return if (allTypes.isEmpty()) {
                    PType.dynamic()
                } else if (allTypes.size == 1) {
                    fromStaticType(allTypes.stream().findFirst().get())
                } else {
                    PType.dynamic()
                }
                //            if (allTypes.stream().allMatch((subType) -> subType instanceof CollectionType)) {}
            }
            is BagType -> {
                val elementType = fromStaticType(type.elementType)
                return PType.bag(elementType)
            }
            is BlobType -> {
                return PType.blob(Int.MAX_VALUE) // TODO: Update this
            }
            is BoolType -> {
                return PType.bool()
            }
            is ClobType -> {
                return PType.clob(Int.MAX_VALUE) // TODO: Update this
            }
            is DateType -> {
                return PType.date()
            }
            is DecimalType -> {
                val precScale = type.precisionScaleConstraint
                if (precScale is PrecisionScaleConstraint.Unconstrained) {
                    return PType.decimal()
                } else if (precScale is PrecisionScaleConstraint.Constrained) {
                    val precisionScaleConstraint = precScale
                    return PType.decimal(precisionScaleConstraint.precision, precisionScaleConstraint.scale)
                } else {
                    throw IllegalStateException()
                }
            }
            is FloatType -> {
                return PType.doublePrecision()
            }
            is IntType -> {
                val cons = type.rangeConstraint
                return when (cons) {
                    IntRangeConstraint.INT4 -> {
                        PType.integer()
                    }
                    IntRangeConstraint.SHORT -> {
                        PType.smallint()
                    }
                    IntRangeConstraint.LONG -> {
                        PType.bigint()
                    }
                    IntRangeConstraint.UNCONSTRAINED -> {
                        PType.numeric()
                    }
                    else -> {
                        throw IllegalStateException()
                    }
                }
            }
            is ListType -> {
                val elementType = fromStaticType(type.elementType)
                return PType.array(elementType)
            }
            is SexpType -> {
                error("PType does not support StaticType SexpType")
            }
            is StringType -> {
                return PType.string()
            }
            is StructType -> {
                val isOrdered = type.constraints.contains(TupleConstraint.Ordered)
                val isClosed = type.contentClosed
                val fields = type.fields.stream().map<Field> { field: StructType.Field ->
                    Field.of(
                        field.key, fromStaticType(field.value)
                    )
                }.collect(Collectors.toList<Field>())
                return if (isClosed && isOrdered) {
                    PType.row(fields)
                } else if (isClosed) {
                    PType.row(fields) // TODO: We currently use ROW when closed.
                } else {
                    PType.struct()
                }
            }
            is SymbolType -> {
                error("PType does not support StaticType SymbolType")
            }
            is TimeType -> {
                var precision = type.precision
                if (precision == null) {
                    precision = 6
                }
                return PType.time(precision)
            }
            is TimestampType -> {
                var precision = type.precision
                if (precision == null) {
                    precision = 6
                }
                return PType.timestamp(precision)
            }
            else -> {
                throw IllegalStateException("Unsupported type: $type")
            }
        }
    }

    // /**
    //  * Create PType from the AST type.
    //  */
    // @JvmStatic
    // fun from(type: Type): PType = when (type) {
    //     is Type.NullType -> error("Casting to NULL is not supported.")
    //     is Type.Missing -> error("Casting to MISSING is not supported.")
    //     is Type.Bool -> bool()
    //     is Type.Tinyint -> tinyint()
    //     is Type.Smallint, is Type.Int2 -> smallint()
    //     is Type.Int4, is Type.Int -> int()
    //     is Type.Bigint, is Type.Int8 -> bigint()
    //     is Type.Numeric -> numeric(type.precision, type.scale)
    //     is Type.Decimal -> decimal(type.precision, type.scale)
    //     is Type.Real -> real()
    //     is Type.Float32 -> real()
    //     is Type.Float64 -> double()
    //     is Type.Char -> char(type.length)
    //     is Type.Varchar -> varchar(type.length)
    //     is Type.String -> string()
    //     is Type.Symbol -> {
    //         // TODO will we continue supporting symbol?
    //         PType.typeSymbol()
    //     }
    //     is Type.Bit -> error("BIT is not supported yet.")
    //     is Type.BitVarying -> error("BIT VARYING is not supported yet.")
    //     is Type.ByteString -> error("BINARY is not supported yet.")
    //     is Type.Blob -> blob(type.length)
    //     is Type.Clob -> clob(type.length)
    //     is Type.Date -> date()
    //     is Type.Time -> time(type.precision)
    //     is Type.TimeWithTz -> timez(type.precision)
    //     is Type.Timestamp -> timestamp(type.precision)
    //     is Type.TimestampWithTz -> timestampz(type.precision)
    //     is Type.Interval -> error("INTERVAL is not supported yet.")
    //     is Type.Bag -> bag()
    //     is Type.Sexp -> {
    //         // TODO will we continue supporting s-expression?
    //         PType.typeSexp()
    //     }
    //     is Type.Any -> dynamic()
    //     is Type.List -> array()
    //     is Type.Tuple -> struct()
    //     is Type.Struct -> struct()
    //     is Type.Custom -> TODO("Custom type not supported ")
    // }
}
