package org.partiql.beam

import org.partiql.beam.io.Beam
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.NumberConstraint
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLTimestampExperimental

/**
 * Convert a static type to a Beam Shape for serde purposes.
 */
public fun StaticType.beam(): Beam.Shape = when (this) {
    is AnyOfType -> beam()
    is AnyType -> Beam.Shape.TDynamic
    is BlobType -> Beam.Shape.TBlob
    is BoolType -> Beam.Shape.TBool
    is ClobType -> Beam.Shape.TClob
    is BagType -> beam()
    is ListType -> beam()
    is SexpType -> beam()
    is DateType -> Beam.Shape.TDate
    is DecimalType -> beam()
    is FloatType -> beam()
    is IntType -> beam()
    MissingType -> Beam.Shape.TMissing
    is NullType -> Beam.Shape.TNull
    is StringType -> beam()
    is StructType -> beam()
    is SymbolType -> Beam.Shape.TSymbol
    is TimeType -> beam()
    is TimestampType -> beam()
    else -> error("unsupported type $this") // graph
}

private fun AnyOfType.beam(): Beam.Shape.TUnion {
    val shapes = ArrayList<Beam.Shape>()
    // create some predictable ordering
    val sorted = this.types.sortedWith { t1, t2 -> t1::class.java.simpleName.compareTo(t2::class.java.simpleName) }
    for (type in sorted) {
        shapes.add(type.beam())
    }
    return Beam.Shape.TUnion(Beam.Shapes(shapes))
}

private fun BagType.beam(): Beam.Shape.TBag {
    return Beam.Shape.TBag(this.elementType.beam())
}

private fun ListType.beam(): Beam.Shape.TList {
    return Beam.Shape.TList(this.elementType.beam())
}

private fun SexpType.beam(): Beam.Shape.TSexp {
    return Beam.Shape.TSexp(this.elementType.beam())
}

private fun DecimalType.beam(): Beam.Shape {
    return when (precisionScaleConstraint) {
        is DecimalType.PrecisionScaleConstraint.Unconstrained -> {
            Beam.Shape.TDecimal
        }
        is DecimalType.PrecisionScaleConstraint.Constrained -> {
            val p = precisionScaleConstraint.precision.toLong()
            val s = precisionScaleConstraint.scale.toLong()
            Beam.Shape.TNumeric(p, s)
        }
    }
}

private fun FloatType.beam(): Beam.Shape {
    // StaticType does not have float constraints.
    return Beam.Shape.TFloat64
}

private fun IntType.beam(): Beam.Shape {
    return when (this.rangeConstraint) {
        IntType.IntRangeConstraint.SHORT -> Beam.Shape.TInt16
        IntType.IntRangeConstraint.INT4 -> Beam.Shape.TInt32
        IntType.IntRangeConstraint.LONG -> Beam.Shape.TInt64
        IntType.IntRangeConstraint.UNCONSTRAINED -> Beam.Shape.TInteger
    }
}

private fun StringType.beam(): Beam.Shape {
    return when (lengthConstraint) {
        is StringType.StringLengthConstraint.Constrained -> {
            when (lengthConstraint.length) {
                is NumberConstraint.Equals -> Beam.Shape.TCharFixed(lengthConstraint.length.value.toLong())
                is NumberConstraint.UpTo -> Beam.Shape.TCharVarying(lengthConstraint.length.value.toLong())
            }
        }
        is StringType.StringLengthConstraint.Unconstrained -> Beam.Shape.TString
    }
}

private fun StructType.beam(): Beam.Shape.TStruct {
    var isClosed = false
    var isOrdered = false
    var hasUniqueKeys = false
    var fields = ArrayList<Beam.Field>()
    for (field in this.fields) {
        fields.add(
            Beam.Field(
                name = field.key,
                shape = field.value.beam(),
            )
        )
    }
    for (constraint in constraints) {
        when (constraint) {
            is TupleConstraint.Open -> isClosed = true
            is TupleConstraint.Ordered -> isOrdered = true
            is TupleConstraint.UniqueAttrs -> hasUniqueKeys = true
        }
    }
    return Beam.Shape.TStruct(
        isClosed = isClosed,
        isOrdered = isOrdered,
        hasUniqueFields = hasUniqueKeys,
        fields = Beam.Fields(fields),
    )
}

private fun TimeType.beam(): Beam.Shape {
    if (precision == null) {
        error("Time precision is required")
    }
    return when (withTimeZone) {
        true -> Beam.Shape.TTime(
            precision = precision.toLong(),
        )
        else -> Beam.Shape.TTimeTz(
            precision = precision.toLong(),
            offsetHour = 0L,
            offsetMinute = 0L,
        )
    }
}

@OptIn(PartiQLTimestampExperimental::class)
private fun TimestampType.beam(): Beam.Shape {
    if (precision == null) {
        error("Time precision is required")
    }
    return when (withTimeZone) {
        true -> Beam.Shape.TTime(
            precision = precision.toLong(),
        )
        else -> Beam.Shape.TTimeTz(
            precision = precision.toLong(),
            offsetHour = 0L,
            offsetMinute = 0L,
        )
    }
}
