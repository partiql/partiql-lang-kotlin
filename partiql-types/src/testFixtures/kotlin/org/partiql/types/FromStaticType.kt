package org.partiql.types

import java.util.stream.Collectors

/**
 * @return a corresponding PType from a [StaticType]
 */
@Deprecated(
    """this API is experimental and is subject to modification/deletion without prior notice. This is
      meant for use internally by the PartiQL library. Public consumers should not use this API."""
)
fun fromStaticType(type: StaticType): PType {
    if (type is AnyType) {
        return PType.dynamic()
    } else if (type is AnyOfType) {
        val allTypes = HashSet(type.flatten().allTypes)
        return if (allTypes.isEmpty()) {
            PType.dynamic()
        } else if (allTypes.size == 1) {
            fromStaticType(allTypes.stream().findFirst().get())
        } else {
            PType.dynamic()
        }
        //            if (allTypes.stream().allMatch((subType) -> subType instanceof CollectionType)) {}
    } else if (type is BagType) {
        val elementType = fromStaticType(type.elementType)
        return PType.bag(elementType)
    } else if (type is BlobType) {
        return PType.blob(Int.MAX_VALUE) // TODO: Update this
    } else if (type is BoolType) {
        return PType.bool()
    } else if (type is ClobType) {
        return PType.clob(Int.MAX_VALUE) // TODO: Update this
    } else if (type is DateType) {
        return PType.date()
    } else if (type is DecimalType) {
        val precScale = type.precisionScaleConstraint
        if (precScale is DecimalType.PrecisionScaleConstraint.Unconstrained) {
            return PType.decimal()
        } else if (precScale is DecimalType.PrecisionScaleConstraint.Constrained) {
            val precisionScaleConstraint = precScale
            return PType.decimal(precisionScaleConstraint.precision, precisionScaleConstraint.scale)
        } else {
            throw IllegalStateException()
        }
    } else if (type is FloatType) {
        return PType.doublePrecision()
    } else if (type is IntType) {
        val cons = type.rangeConstraint
        return if (cons == IntType.IntRangeConstraint.INT4) {
            PType.integer()
        } else if (cons == IntType.IntRangeConstraint.SHORT) {
            PType.smallint()
        } else if (cons == IntType.IntRangeConstraint.LONG) {
            PType.bigint()
        } else if (cons == IntType.IntRangeConstraint.UNCONSTRAINED) {
            PType.numeric()
        } else {
            throw IllegalStateException()
        }
    } else if (type is ListType) {
        val elementType = fromStaticType(type.elementType)
        return PType.array(elementType)
    } else if (type is SexpType) {
        val elementType = fromStaticType(type.elementType)
        return PType.sexp(elementType)
    } else if (type is StringType) {
        return PType.string()
    } else if (type is StructType) {
        val isOrdered = type.constraints.contains(TupleConstraint.Ordered)
        val isClosed = type.contentClosed
        val fields = type.fields.stream().map { field: StructType.Field ->
            Field.of(
                field.key,
                fromStaticType(field.value)
            )
        }.collect(Collectors.toList())
        return if (isClosed && isOrdered) {
            PType.row(fields)
        } else if (isClosed) {
            PType.row(fields) // TODO: We currently use ROW when closed.
        } else {
            PType.struct()
        }
    } else if (type is SymbolType) {
        return PType.symbol()
    } else if (type is TimeType) {
        var precision = type.precision
        if (precision == null) {
            precision = 6
        }
        return PType.time(precision)
    } else if (type is TimestampType) {
        var precision = type.precision
        if (precision == null) {
            precision = 6
        }
        return PType.timestamp(precision)
    } else {
        throw IllegalStateException("Unsupported type: $type")
    }
}
