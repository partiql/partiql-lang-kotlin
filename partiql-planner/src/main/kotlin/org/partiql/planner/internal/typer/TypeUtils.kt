package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Rel
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.CollectionType
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

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)

internal fun StaticType.isNumeric(): Boolean = (this is IntType || this is FloatType || this is DecimalType)

internal fun StaticType.isExactNumeric(): Boolean = (this is IntType || this is DecimalType)

internal fun StaticType.isApproxNumeric(): Boolean = (this is FloatType)

internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)

internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

internal fun StaticType.isOptional(): Boolean = when (this) {
    is AnyType, MissingType -> true // Any includes Missing type
    is AnyOfType -> types.any { it.isOptional() }
    else -> false
}

/**
 * Per SQL, runtime types are always nullable
 */
@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLValueType.toStaticType(): StaticType = when (this) {
    PartiQLValueType.NULL -> StaticType.NULL
    PartiQLValueType.MISSING -> StaticType.MISSING
    else -> toNonNullStaticType().asNullable()
}

@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLValueType.toNonNullStaticType(): StaticType = when (this) {
    PartiQLValueType.ANY -> StaticType.ANY
    PartiQLValueType.BOOL -> StaticType.BOOL
    PartiQLValueType.INT8 -> StaticType.INT2
    PartiQLValueType.INT16 -> StaticType.INT2
    PartiQLValueType.INT32 -> StaticType.INT4
    PartiQLValueType.INT64 -> StaticType.INT8
    PartiQLValueType.INT -> StaticType.INT
    PartiQLValueType.DECIMAL_ARBITRARY -> StaticType.DECIMAL
    PartiQLValueType.DECIMAL -> StaticType.DECIMAL
    PartiQLValueType.FLOAT32 -> StaticType.FLOAT
    PartiQLValueType.FLOAT64 -> StaticType.FLOAT
    PartiQLValueType.CHAR -> StaticType.CHAR
    PartiQLValueType.STRING -> StaticType.STRING
    PartiQLValueType.SYMBOL -> StaticType.SYMBOL
    PartiQLValueType.BINARY -> TODO()
    PartiQLValueType.BYTE -> TODO()
    PartiQLValueType.BLOB -> StaticType.BLOB
    PartiQLValueType.CLOB -> StaticType.CLOB
    PartiQLValueType.DATE -> StaticType.DATE
    PartiQLValueType.TIME -> StaticType.TIME
    PartiQLValueType.TIMESTAMP -> StaticType.TIMESTAMP
    PartiQLValueType.INTERVAL -> TODO()
    PartiQLValueType.BAG -> StaticType.BAG
    PartiQLValueType.LIST -> StaticType.LIST
    PartiQLValueType.SEXP -> StaticType.SEXP
    PartiQLValueType.STRUCT -> StaticType.STRUCT
    PartiQLValueType.NULL -> StaticType.NULL
    PartiQLValueType.MISSING -> StaticType.MISSING
}

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
internal fun StaticType.toRuntimeTypeOrNull(): PartiQLValueType? {
    return try {
        this.toRuntimeType()
    } catch (_: Throwable) {
        null
    }
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

/**
 * Applies the given exclusion path to produce the reduced [StaticType]. [lastStepOptional] indicates if a previous
 * step in the exclude path includes a collection index exclude step. Currently, for paths with the last step as
 * a struct symbol/key, the type inference will define that struct value as optional if [lastStepOptional] is true.
 * Note, this specific behavior could change depending on `EXCLUDE`'s static typing behavior in a future RFC.
 *
 * e.g. EXCLUDE t.a[1].field_x will define the struct value `field_x` as optional
 *
 * @param steps
 * @param lastStepOptional
 * @return
 */
internal fun StaticType.exclude(steps: List<Rel.Op.Exclude.Step>, lastStepOptional: Boolean = false): StaticType {
    val type = this
    return steps.fold(type) { acc, step ->
        when (acc) {
            is StructType -> acc.exclude(step, lastStepOptional)
            is CollectionType -> acc.exclude(step, lastStepOptional)
            is AnyOfType -> StaticType.unionOf(
                acc.types.map { it.exclude(steps, lastStepOptional) }.toSet()
            )
            else -> acc
        }.flatten()
    }
}

/**
 * Applies exclusions to struct fields.
 *
 * @param step
 * @param lastStepOptional
 * @return
 */
internal fun StructType.exclude(step: Rel.Op.Exclude.Step, lastStepOptional: Boolean = false): StaticType {
    val type = step.type
    val substeps = step.substeps
    val output = fields.mapNotNull { field ->
        val newField = if (substeps.isEmpty()) {
            if (lastStepOptional) {
                StructType.Field(field.key, field.value.asOptional())
            } else {
                null
            }
        } else {
            val k = field.key
            val v = field.value.exclude(substeps, lastStepOptional)
            StructType.Field(k, v)
        }
        when (type) {
            is Rel.Op.Exclude.Type.StructSymbol -> {
                if (type.symbol.equals(field.key, ignoreCase = true)) {
                    newField
                } else {
                    field
                }
            }

            is Rel.Op.Exclude.Type.StructKey -> {
                if (type.key == field.key) {
                    newField
                } else {
                    field
                }
            }
            is Rel.Op.Exclude.Type.StructWildcard -> newField
            else -> field
        }
    }
    return this.copy(fields = output)
}

/**
 * Applies exclusions to collection element type.
 *
 * @param step
 * @param lastStepOptional
 * @return
 */
internal fun CollectionType.exclude(step: Rel.Op.Exclude.Step, lastStepOptional: Boolean = false): StaticType {
    var e = this.elementType
    val substeps = step.substeps
    when (step.type) {
        is Rel.Op.Exclude.Type.CollIndex -> {
            if (substeps.isNotEmpty()) {
                e = e.exclude(substeps, lastStepOptional = true)
            }
        }
        is Rel.Op.Exclude.Type.CollWildcard -> {
            if (substeps.isNotEmpty()) {
                e = e.exclude(substeps, lastStepOptional)
            }
            // currently no change to elementType if collection wildcard is last element; this behavior could
            // change based on RFC definition
        }
        else -> {
            // currently no change to elementType and no error thrown; could consider an error/warning in
            // the future
        }
    }
    return when (this) {
        is BagType -> this.copy(e)
        is ListType -> this.copy(e)
        is SexpType -> this.copy(e)
    }
}
