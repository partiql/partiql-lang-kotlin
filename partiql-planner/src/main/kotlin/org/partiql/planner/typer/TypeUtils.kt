package org.partiql.planner.typer

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
import org.partiql.types.PartiQLValueType
import org.partiql.types.SexpType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint
import org.partiql.types.UnsupportedTypeCheckException
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import java.math.BigDecimal

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)

internal fun StaticType.isNumeric(): Boolean = (this is IntType || this is FloatType || this is DecimalType)

internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)

internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

/**
 * TypeUtils extends StaticType to provide additionally functionality as well as
 */
@OptIn(PartiQLValueExperimental::class)
internal object TypeUtils {

    /**
     * Checks to see if the given [PartiQLValue] conforms to the current [StaticType].
     *
     * For [SingleType], provides a default implementation of [isInstance] that returns true
     * if the [PartiQLValueType] of [value] is the same as the [runtimeType].
     *
     * That is appropriate for all [SingleType]-derived classes that do not have specific constraints but this must be
     * overridden in any [SingleType]s that have constraints such as [IntType], [StringType] and [DecimalType].
     * [NullType] also has custom implementation of this function to support `MISSING IS NULL` semantics.
     *
     * Throwing [UnsupportedTypeCheckException] is temporary while some classes derived from [StaticType]
     * do not fully implement this function and is thrown when the encounter situations that can't yet be checked.
     */
    @JvmStatic
    @Throws(UnsupportedTypeCheckException::class)
    public fun isInstance(value: PartiQLValue, type: StaticType): Boolean = when (type) {
        is AnyType -> true
        is CollectionType -> {
            if (value.type != getRuntimeType(type)) {
                false
            } else {
                when (type.elementType) {
                    StaticType.ANY -> true
                    else -> value.all { isInstance(it, type.elementType) }
                }
            }
        }
        is IntType -> {
            if (value.type != PartiQLValueType.INT) {
                false
            } else {
                val longValue = value.numberValue().toLong()
                type.rangeConstraint.validRange.contains(longValue)
            }
        }
        is DecimalType -> {
            when (value.type) {
                PartiQLValueType.DECIMAL -> type.precisionScaleConstraint.matches(value.scalar.numberValue() as BigDecimal)
                else -> false
            }
        }
        is NullType -> value.type == PartiQLValueType.NULL || value.type == PartiQLValueType.MISSING
        is StringType -> when (value.type) {
            PartiQLValueType.STRING -> stringLengthConstraintMatches(type.lengthConstraint, value)
            else -> false
        }
        is StructType -> type.isInstanceOf(value)
        is SingleType -> value.type == getRuntimeType(type)
        is AnyOfType -> type.types.any { isInstance(value, it) }
    }

    @JvmStatic
    public fun getTypeDomain(type: StaticType): Set<PartiQLValueType> = when (type) {
        is AnyType -> enumValues<PartiQLValueType>().toSet()
        is SingleType -> setOf(getRuntimeType(type))
        is AnyOfType -> type.types.flatMap { getTypeDomain(it) }.toSet()
    }

    @JvmStatic
    public fun getRuntimeType(type: SingleType): PartiQLValueType = when (type) {
        is BlobType -> PartiQLValueType.BLOB
        is BoolType -> PartiQLValueType.BOOL
        is ClobType -> PartiQLValueType.CLOB
        is BagType -> PartiQLValueType.BAG
        is ListType -> PartiQLValueType.LIST
        is SexpType -> PartiQLValueType.SEXP
        is DateType -> PartiQLValueType.DATE
        is DecimalType -> PartiQLValueType.DECIMAL
        is FloatType -> PartiQLValueType.FLOAT64
        is IntType -> PartiQLValueType.INT
        MissingType -> PartiQLValueType.MISSING
        is NullType -> PartiQLValueType.NULL
        is StringType -> PartiQLValueType.STRING
        is StructType -> PartiQLValueType.STRUCT
        is SymbolType -> PartiQLValueType.SYMBOL
        is TimeType -> PartiQLValueType.TIME
        is TimestampType -> PartiQLValueType.TIMESTAMP
        is GraphType -> PartiQLValueType.GRAPH
    }

    /**
     * Returns true if [other] [StaticType] is comparable to the current [StaticType]. Currently, two types are
     * comparable if
     *  - they are both numeric
     *  - they are both text
     *  - they are both lobs
     *  - they are both [SingleType]s with the same [SingleType.runtimeType]
     *  - one [StaticType] from current [StaticType]'s [allTypes] is comparable to a [StaticType] from [other]'s
     *   [allTypes]
     */
    @JvmStatic
    public fun areStaticTypesComparable(lhs: StaticType, rhs: StaticType): Boolean {
        return when (lhs) {
            is SingleType -> {
                if (lhs.isUnknown() || rhs.isUnknown()) {
                    return true
                }
                when (rhs) {
                    is SingleType -> (lhs.isNumeric() && rhs.isNumeric()) ||
                        (lhs.isText() && rhs.isText()) ||
                        (lhs.isLob() && rhs.isLob()) ||
                        (getRuntimeType(lhs) == getRuntimeType(rhs))
                    is AnyType -> true
                    is AnyOfType -> rhs.allTypes.filter { !it.isUnknown() }.any { areStaticTypesComparable(lhs, it) }
                }
            }
            is AnyType -> true
            is AnyOfType -> {
                if (lhs.isUnknown() || rhs.isUnknown()) {
                    return true
                }

                val typesA = lhs.allTypes.filter { !it.isUnknown() }
                val typesB = rhs.allTypes.filter { !it.isUnknown() }

                typesA.forEach { tA ->
                    typesB.forEach { tB ->
                        if (areStaticTypesComparable(tA, tB)) {
                            return true
                        }
                    }
                }
                return false
            }
        }
    }

    @JvmStatic
    public fun staticTypeFromPartiQLValueType(exprValueType: PartiQLValueType): StaticType =
        when (exprValueType) {
            PartiQLValueType.MISSING -> StaticType.MISSING
            PartiQLValueType.NULL -> StaticType.NULL
            PartiQLValueType.BOOL -> StaticType.BOOL
            PartiQLValueType.INT -> StaticType.INT
            PartiQLValueType.FLOAT64 -> StaticType.FLOAT
            PartiQLValueType.DECIMAL -> StaticType.DECIMAL
            PartiQLValueType.DATE -> StaticType.DATE
            PartiQLValueType.TIME -> StaticType.TIME
            PartiQLValueType.TIMESTAMP -> StaticType.TIMESTAMP
            PartiQLValueType.SYMBOL -> StaticType.SYMBOL
            PartiQLValueType.STRING -> StaticType.STRING
            PartiQLValueType.CLOB -> StaticType.CLOB
            PartiQLValueType.BLOB -> StaticType.BLOB
            PartiQLValueType.LIST -> StaticType.LIST
            PartiQLValueType.SEXP -> StaticType.SEXP
            PartiQLValueType.STRUCT -> StaticType.STRUCT
            PartiQLValueType.BAG -> StaticType.BAG
            PartiQLValueType.GRAPH -> StaticType.GRAPH
        }

    @JvmStatic
    public fun staticTypeFromPartiQLValue(value: PartiQLValue) = when (value.type) {
        PartiQLValueType.TIME -> {
            val timeValue = value.timeValue()
            TimeType(precision = timeValue.precision, withTimeZone = timeValue.zoneOffset != null)
        }
        else -> staticTypeFromPartiQLValueType(value.type)
    }

    /**
     * Checks if this subtype of the given [StaticType].
     *
     * A [StaticType] is subtype of another iff if its type domain in equal or smaller than the given type.
     */
    @JvmStatic
    fun isSubTypeOf(child: StaticType, parent: StaticType): Boolean {
        return getTypeDomain(child).isNotEmpty() && getTypeDomain(parent).containsAll(getTypeDomain(child))
    }

    @JvmStatic
    public fun stringLengthConstraintMatches(constraint: StringType.StringLengthConstraint, value: PartiQLValue): Boolean {
        return when (constraint) {
            StringType.StringLengthConstraint.Unconstrained -> true
            is StringType.StringLengthConstraint.Constrained -> {
                val str = value.scalar.stringValue()
                    ?: error("value.scalar.stringValue() unexpectedly returned null")
                return constraint.length.matches(str.codePointCount(0, str.length))
            }
        }
    }

    //
    //
    // HELPER METHODS
    //
    //

    /**
     * By far, structs have the most complicated logic behind their instance check.
     *
     * This method returns true if:
     *
     * - All fields are instance of the correct type (as identified by [fields]).
     * - If [contentClosed] and there are no fields other than those listed in [fields].
     *
     * Duplicate fields are supported, but all instances of a field with the same name
     * must match the type specified in [fields].
     *
     * If the struct contains any non-text key, this automatically means that the struct is not an instance of this
     * [StructType].  We do not even have the ability to model that with Ion/Ion Schema anyway.
     */
    private fun StructType.isInstanceOf(value: PartiQLValue) = when {
        value.type != PartiQLValueType.STRUCT -> false
        fields.isEmpty() && !contentClosed -> true
        this.constraints.contains(TupleConstraint.Ordered) && value.asFacet(OrderedBindNames::class.java) == null -> false
        else -> {
            when (this.constraints.contains(TupleConstraint.Ordered)) {
                false -> {
                    // build a multi-map of fields in the struct.
                    val scratchPad = HashMap<String, MutableList<PartiQLValue>>().also { map ->
                        value.forEach { v ->
                            // return false early if the struct key is not a string or symbol.
                            val structKey = v.name.takeIf { it?.type?.isText ?: false } ?: return false
                            map.getOrPut(structKey.stringValue()) { ArrayList() }.add(v)
                        }
                    }

                    // Consolidates fields to check that the PartiQLValue sufficiently conforms to the StaticType
                    //  defined in the consolidated field
                    val consolidatedFields = fields.groupBy({ it.key }) { it.value }.map {
                        StructType.Field(
                            it.key,
                            StaticType.unionOf(it.value.toSet()).flatten()
                        )
                    }

                    // now go thru each of the [fields] and remove those that are valid
                    consolidatedFields.forEach { (fieldName, fieldType) ->
                        val fieldValues = scratchPad.remove(fieldName)

                        // Field was *not* present
                        if (fieldValues == null) {
                            // if field was required, the struct is not an instance of this [StructType]
                            if (!fieldType.isOptional()) {
                                return false
                            }
                            // else there is no violation, keep checking other fields
                        } else {
                            // in the case of multiple fields with the same name, all values must match
                            if (!fieldValues.all { isInstance(it, fieldType) }) {
                                return false
                            }
                            // else there is no violation, keep checking other fields
                        }
                    }

                    // if we reach this point, we didn't find any fields that do not comply with their final types.

                    // If no fields remain [value] is an instance of this [StaticType]
                    if (scratchPad.none()) {
                        true
                    } else {
                        // There are some fields left over, so we only need to check if we are closedContent or not.
                        !contentClosed
                    }
                }
                true -> {
                    value.asFacet(OrderedBindNames::class.java)?.orderedNames.let { orderedNames ->
                        if (orderedNames == null) { return false }
                        val fieldNames = this.fields.map { it.key }
                        if (fieldNames != orderedNames) { return false }
                    }
                    this.fields.forEachIndexed { index, field ->
                        val attrValue = value.ordinalBindings[index] ?: return false
                        if (isInstance(attrValue, field.value).not()) {
                            return false
                        }
                    }
                    true
                }
            }
        }
    }

    internal fun StaticType.isOptional(): Boolean =
        when (this) {
            is AnyType, MissingType -> true // Any includes Missing type
            is AnyOfType -> types.any { it.isOptional() }
            else -> false
        }
}
