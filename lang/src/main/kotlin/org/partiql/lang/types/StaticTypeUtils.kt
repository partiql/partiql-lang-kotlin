package org.partiql.lang.types

import org.partiql.lang.ast.passes.inference.isLob
import org.partiql.lang.ast.passes.inference.isNumeric
import org.partiql.lang.ast.passes.inference.isText
import org.partiql.lang.ast.passes.inference.isUnknown
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.name
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.timeValue
import org.partiql.spi.types.AnyOfType
import org.partiql.spi.types.AnyType
import org.partiql.spi.types.BagType
import org.partiql.spi.types.BlobType
import org.partiql.spi.types.BoolType
import org.partiql.spi.types.ClobType
import org.partiql.spi.types.CollectionType
import org.partiql.spi.types.DateType
import org.partiql.spi.types.DecimalType
import org.partiql.spi.types.FloatType
import org.partiql.spi.types.IntType
import org.partiql.spi.types.ListType
import org.partiql.spi.types.MissingType
import org.partiql.spi.types.NullType
import org.partiql.spi.types.SexpType
import org.partiql.spi.types.SingleType
import org.partiql.spi.types.StaticType
import org.partiql.spi.types.StringType
import org.partiql.spi.types.StructType
import org.partiql.spi.types.SymbolType
import org.partiql.spi.types.TimeType
import org.partiql.spi.types.TimestampType
import org.partiql.spi.types.UnsupportedTypeCheckException
import java.math.BigDecimal

public object StaticTypeUtils {

    /**
     * For [SingleType], provides a default implementation of [isInstance] that returns true
     * if the [ExprValueType] of [value] is the same as the [runtimeType].
     *
     * That is appropriate for all [SingleType]-derived classes that do not have specific constraints but this must be
     * overridden in any [SingleType]s that have constraints such as [IntType], [StringType] and [DecimalType].
     * [NullType] also has custom implementation of this function to support `MISSING IS NULL` semantics.
     */
    /**
     * Checks to see if the given [ExprValue] conforms to the current [StaticType].
     *
     * Throwing [UnsupportedTypeCheckException] is temporary while some classes derived from [StaticType]
     * do not fully implement this function and is thrown when the encounter situations that can't yet be checked.
     */
    @JvmStatic
    @Throws(UnsupportedTypeCheckException::class)
    public fun isInstance(value: ExprValue, type: StaticType): Boolean = when (type) {
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
            if (value.type != ExprValueType.INT) {
                false
            } else {
                val longValue = value.numberValue().toLong()
                type.rangeConstraint.validRange.contains(longValue)
            }
        }
        is DecimalType -> {
            when (value.type) {
                ExprValueType.DECIMAL -> type.precisionScaleConstraint.matches(value.scalar.numberValue() as BigDecimal)
                else -> false
            }
        }
        is NullType -> value.type == ExprValueType.NULL || value.type == ExprValueType.MISSING
        is StringType -> when (value.type) {
            ExprValueType.STRING -> stringLengthConstraintMatches(type.lengthConstraint, value)
            else -> false
        }
        is StructType -> type.isInstanceOf(value)
        is SingleType -> value.type == getRuntimeType(type)
        is AnyOfType -> type.types.any { isInstance(value, it) }
    }

    @JvmStatic
    public fun getTypeDomain(type: StaticType): Set<ExprValueType> = when (type) {
        is AnyType -> enumValues<ExprValueType>().toSet()
        is SingleType -> setOf(getRuntimeType(type))
        is AnyOfType -> type.types.flatMap { getTypeDomain(it) }.toSet()
    }

    @JvmStatic
    public fun getRuntimeType(type: SingleType): ExprValueType = when (type) {
        is BlobType -> ExprValueType.BLOB
        is BoolType -> ExprValueType.BOOL
        is ClobType -> ExprValueType.CLOB
        is BagType -> ExprValueType.BAG
        is ListType -> ExprValueType.LIST
        is SexpType -> ExprValueType.SEXP
        is DateType -> ExprValueType.DATE
        is DecimalType -> ExprValueType.DECIMAL
        is FloatType -> ExprValueType.FLOAT
        is IntType -> ExprValueType.INT
        MissingType -> ExprValueType.MISSING
        is NullType -> ExprValueType.NULL
        is StringType -> ExprValueType.STRING
        is StructType -> ExprValueType.STRUCT
        is SymbolType -> ExprValueType.SYMBOL
        is TimeType -> ExprValueType.TIME
        is TimestampType -> ExprValueType.TIMESTAMP
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
    public fun staticTypeFromExprValueType(exprValueType: ExprValueType): StaticType =
        when (exprValueType) {
            ExprValueType.MISSING -> StaticType.MISSING
            ExprValueType.NULL -> StaticType.NULL
            ExprValueType.BOOL -> StaticType.BOOL
            ExprValueType.INT -> StaticType.INT
            ExprValueType.FLOAT -> StaticType.FLOAT
            ExprValueType.DECIMAL -> StaticType.DECIMAL
            ExprValueType.DATE -> StaticType.DATE
            ExprValueType.TIME -> StaticType.TIME
            ExprValueType.TIMESTAMP -> StaticType.TIMESTAMP
            ExprValueType.SYMBOL -> StaticType.SYMBOL
            ExprValueType.STRING -> StaticType.STRING
            ExprValueType.CLOB -> StaticType.CLOB
            ExprValueType.BLOB -> StaticType.BLOB
            ExprValueType.LIST -> StaticType.LIST
            ExprValueType.SEXP -> StaticType.SEXP
            ExprValueType.STRUCT -> StaticType.STRUCT
            ExprValueType.BAG -> StaticType.BAG
        }

    @JvmStatic
    public fun staticTypeFromExprValue(value: ExprValue) = when (value.type) {
        ExprValueType.TIME -> {
            val timeValue = value.timeValue()
            TimeType(precision = timeValue.precision, withTimeZone = timeValue.zoneOffset != null)
        }
        else -> staticTypeFromExprValueType(value.type)
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
    public fun stringLengthConstraintMatches(constraint: StringType.StringLengthConstraint, value: ExprValue): Boolean {
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
    private fun StructType.isInstanceOf(value: ExprValue) = when {
        fields.isEmpty() && !contentClosed -> value.type == ExprValueType.STRUCT
        else -> {
            if (value.type != ExprValueType.STRUCT) {
                false
            } else {
                // build a multi-map of fields in the struct.
                val scratchPad = HashMap<String, MutableList<ExprValue>>().also { map ->
                    value.forEach { v ->
                        // return false early if the struct key is not a string or symbol.
                        val structKey = v.name.takeIf { it?.type?.isText ?: false } ?: return false
                        map.getOrPut(structKey.stringValue()) { ArrayList() }.add(v)
                    }
                }

                // now go thru each of the [fields] and remove those that are valid
                fields.forEach { (fieldName, fieldType) ->
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
        }
    }

    internal fun StaticType.isOptional(): Boolean =
        when (this) {
            is AnyType, MissingType -> true // Any includes Missing type
            is AnyOfType -> types.any { it.isOptional() }
            else -> false
        }
}
