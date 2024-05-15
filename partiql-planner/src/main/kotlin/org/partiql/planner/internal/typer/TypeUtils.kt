package org.partiql.planner.internal.typer

import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
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

@Suppress("DEPRECATION")
@Deprecated(
    message = "This will be removed in a future major-version bump.",
    replaceWith = ReplaceWith("ANY")
)
internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)

internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)

@Suppress("DEPRECATION")
@Deprecated(
    message = "This will be removed in a future major-version bump.",
    replaceWith = ReplaceWith("ANY")
)
internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

/**
 * Returns whether [this] *may* be of a specific type. AKA: is it the type? Is it a union that holds the type?
 */
internal inline fun <reified T> StaticType.mayBeType(): Boolean {
    return this.allTypes.any { it is T }
}

/**
 * For each type in [this] [StaticType.allTypes], the [block] will be invoked. Non-null outputs to the [block] will be
 * returned.
 */
internal fun StaticType.inferListNotNull(block: (StaticType) -> StaticType?): List<StaticType> {
    return this.flatten().allTypes.mapNotNull { type -> block(type) }
}

/**
 * For each type in [this] [StaticType.allTypes], the [block] will be invoked. Non-null outputs to the [block] will be
 * returned.
 */
internal fun StaticType.inferRexListNotNull(block: (StaticType) -> Rex?): List<Rex> {
    return this.flatten().allTypes.mapNotNull { type -> block(type) }
}

/**
 * Per SQL, runtime types are always nullable
 */
@OptIn(PartiQLValueExperimental::class)
@Suppress("DEPRECATION")
internal fun PartiQLValueType.toStaticType(): StaticType = when (this) {
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
    PartiQLValueType.NULL -> StaticType.ANY
    PartiQLValueType.MISSING -> StaticType.ANY
}

@OptIn(PartiQLValueExperimental::class)
@Suppress("DEPRECATION")
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

@Suppress("DEPRECATION")
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
    MissingType -> PartiQLValueType.ANY
    is NullType -> PartiQLValueType.ANY
    is StringType -> PartiQLValueType.STRING
    is StructType -> PartiQLValueType.STRUCT
    is SymbolType -> PartiQLValueType.SYMBOL
    is TimeType -> PartiQLValueType.TIME
    is TimestampType -> PartiQLValueType.TIMESTAMP
}

/**
 * Applies the given exclusion path to produce the reduced StaticType
 *
 * @param steps
 * @param lastStepOptional
 * @return
 */
internal fun StaticType.exclude(steps: List<Rel.Op.Exclude.Step>, lastStepOptional: Boolean = true): StaticType =
    when (this) {
        is StructType -> this.exclude(steps, lastStepOptional)
        is CollectionType -> this.exclude(steps, lastStepOptional)
        is AnyOfType -> StaticType.unionOf(
            this.types.map { it.exclude(steps, lastStepOptional) }.toSet()
        )
        else -> this
    }.flatten()

/**
 * Applies exclusions to struct fields.
 *
 * @param steps
 * @param lastStepOptional
 * @return
 */
internal fun StructType.exclude(steps: List<Rel.Op.Exclude.Step>, lastStepOptional: Boolean = true): StaticType {
    val step = steps.first()
    val output = fields.map { field ->
        val newField = if (steps.size == 1) {
            if (lastStepOptional) {
                StructType.Field(field.key, field.value) // TODO: double check this
            } else {
                null
            }
        } else {
            val k = field.key
            val v = field.value.exclude(steps.drop(1), lastStepOptional)
            StructType.Field(k, v)
        }
        when (step) {
            is Rel.Op.Exclude.Step.StructField -> {
                if (step.symbol.isEquivalentTo(field.key)) {
                    newField
                } else {
                    field
                }
            }
            is Rel.Op.Exclude.Step.StructWildcard -> newField
            else -> field
        }
    }.filterNotNull()
    return this.copy(fields = output)
}

/**
 * Applies exclusions to collection element type.
 *
 * @param steps
 * @param lastStepOptional
 * @return
 */
internal fun CollectionType.exclude(steps: List<Rel.Op.Exclude.Step>, lastStepOptional: Boolean = true): StaticType {
    var e = this.elementType
    when (steps.first()) {
        is Rel.Op.Exclude.Step.CollIndex -> {
            if (steps.size > 1) {
                e = e.exclude(steps.drop(1), true)
            }
        }
        is Rel.Op.Exclude.Step.CollWildcard -> {
            if (steps.size > 1) {
                e = e.exclude(steps.drop(1), lastStepOptional)
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

/**
 * Compare an identifier to a struct field; handling case-insensitive comparisons.
 *
 * @param other
 * @return
 */
private fun Identifier.Symbol.isEquivalentTo(other: String): Boolean = when (caseSensitivity) {
    Identifier.CaseSensitivity.SENSITIVE -> symbol.equals(other)
    Identifier.CaseSensitivity.INSENSITIVE -> symbol.equals(other, ignoreCase = true)
}
