package org.partiql.eval.internal.helpers

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
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal object TypesUtility {

    @OptIn(PartiQLValueExperimental::class)
    internal fun StaticType.toRuntimeType(): PartiQLValueType {
        if (this is AnyOfType) {
            // handle anyOf(null, T) cases
            val t = types.filter { it !is NullType && it !is MissingType }
            return if (t.size != 1) {
                PartiQLValueType.ANY
            } else {
                t.first().asRuntimeType()
            }
        }
        return this.asRuntimeType()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun StaticType.asRuntimeType(): PartiQLValueType = when (this) {
        is AnyOfType -> PartiQLValueType.ANY
        is AnyType -> PartiQLValueType.ANY
        is BlobType -> PartiQLValueType.BLOB
        is BoolType -> PartiQLValueType.BOOL
        is ClobType -> PartiQLValueType.CLOB
        is BagType -> PartiQLValueType.BAG
        is ListType -> PartiQLValueType.LIST
        is SexpType -> PartiQLValueType.SEXP
        is DateType -> PartiQLValueType.DATE
        // TODO: Run time decimal type does not model precision scale constraint yet
        //  despite that we match to Decimal vs Decimal_ARBITRARY (PVT) here
        //  but when mapping it back to Static Type, (i.e, mapping function return type to Value Type)
        //  we can only map to Unconstrained decimal (Static Type)
        is DecimalType -> {
            when (this.precisionScaleConstraint) {
                is DecimalType.PrecisionScaleConstraint.Constrained -> PartiQLValueType.DECIMAL
                DecimalType.PrecisionScaleConstraint.Unconstrained -> PartiQLValueType.DECIMAL_ARBITRARY
            }
        }
        is FloatType -> PartiQLValueType.FLOAT64
        is GraphType -> error("Graph type missing from runtime types")
        is IntType -> when (this.rangeConstraint) {
            IntType.IntRangeConstraint.SHORT -> PartiQLValueType.INT16
            IntType.IntRangeConstraint.INT4 -> PartiQLValueType.INT32
            IntType.IntRangeConstraint.LONG -> PartiQLValueType.INT64
            IntType.IntRangeConstraint.UNCONSTRAINED -> PartiQLValueType.INT
        }
        MissingType -> PartiQLValueType.MISSING
        is NullType -> PartiQLValueType.NULL
        is StringType -> PartiQLValueType.STRING
        is StructType -> PartiQLValueType.STRUCT
        is SymbolType -> PartiQLValueType.SYMBOL
        is TimeType -> PartiQLValueType.TIME
        is TimestampType -> PartiQLValueType.TIMESTAMP
    }
}
