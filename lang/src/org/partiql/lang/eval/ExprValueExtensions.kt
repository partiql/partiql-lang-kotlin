/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.Timestamp
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.ots_work.stscore.ScalarTypeSystem
import org.partiql.lang.syntax.DATE_TIME_PART_KEYWORDS
import org.partiql.lang.syntax.DateTimePart
import org.partiql.lang.types.BagType
import org.partiql.lang.types.CollectionType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.MissingType
import org.partiql.lang.types.NullType
import org.partiql.lang.types.SexpType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticScalarType
import org.partiql.lang.types.StructType
import org.partiql.lang.util.ConfigurableExprValueFormatter
import org.partiql.lang.util.Time
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.downcast
import org.partiql.lang.util.ionValue
import java.math.BigDecimal
import java.time.LocalDate
import java.util.TreeSet

/**
 * Wraps the given [ExprValue] with a delegate that provides the [OrderedBindNames] facet.
 */
fun ExprValue.orderedNamesValue(names: List<String>): ExprValue =
    object : ExprValue by this, OrderedBindNames {
        override val orderedNames = names
        override fun <T : Any?> asFacet(type: Class<T>?): T? =
            downcast(type) ?: this@orderedNamesValue.asFacet(type)
        override fun toString(): String = stringify()
    }

val ExprValue.orderedNames: List<String>?
    get() = asFacet(OrderedBindNames::class.java)?.orderedNames

/** Wraps this [ExprValue] as a [Named] instance. */
fun ExprValue.asNamed(): Named = object : Named {
    override val name: ExprValue
        get() = this@asNamed
}

/** Binds the given name value as a [Named] facet delegate over this [ExprValue]. */
fun ExprValue.namedValue(nameValue: ExprValue): ExprValue = object : ExprValue by this, Named {
    override val name = nameValue
    override fun <T : Any?> asFacet(type: Class<T>?): T? =
        downcast(type) ?: this@namedValue.asFacet(type)
    override fun toString(): String = stringify()
}

/** Wraps this [ExprValue] in a delegate that always masks the [Named] facet. */
fun ExprValue.unnamedValue(): ExprValue = when (asFacet(Named::class.java)) {
    null -> this
    else -> object : ExprValue by this {
        override fun <T : Any?> asFacet(type: Class<T>?): T? =
            when (type) {
                // always mask the name facet
                Named::class.java -> null
                else -> this@unnamedValue.asFacet(type)
            }
        override fun toString(): String = stringify()
    }
}

val ExprValue.name: ExprValue?
    get() = asFacet(Named::class.java)?.name

val ExprValue.address: ExprValue?
    get() = asFacet(Addressed::class.java)?.address

fun ExprValue.booleanValue(): Boolean =
    scalar.booleanValue() ?: errNoContext("Expected boolean: $ionValue", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.numberValue(): Number =
    scalar.numberValue() ?: errNoContext("Expected number: $ionValue", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.dateValue(): LocalDate =
    scalar.dateValue() ?: errNoContext("Expected date: $ionValue", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.timeValue(): Time =
    scalar.timeValue() ?: errNoContext("Expected time: $ionValue", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.timestampValue(): Timestamp =
    scalar.timestampValue() ?: errNoContext("Expected timestamp: $ionValue", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.stringValue(): String =
    scalar.stringValue() ?: errNoContext("Expected string: $ionValue", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.bytesValue(): ByteArray =
    scalar.bytesValue() ?: errNoContext("Expected boolean: $ionValue", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

internal fun ExprValue.dateTimePartValue(): DateTimePart =
    try {
        DateTimePart.valueOf(this.stringValue().toUpperCase())
    } catch (e: IllegalArgumentException) {
        throw EvaluationException(
            cause = e,
            message = "invalid datetime part, valid values: [${DATE_TIME_PART_KEYWORDS.joinToString()}]",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART,
            internal = false
        )
    }

internal fun ExprValue.intValue(): Int = this.numberValue().toInt()

internal fun ExprValue.longValue(): Long = this.numberValue().toLong()

internal fun ExprValue.bigDecimalValue(): BigDecimal = this.numberValue().toString().toBigDecimal()

/**
 * Implements the `FROM` range operation.
 * Specifically, this is distinct from the normal [ExprValue.iterator] in that
 * types that are **not** [ExprValueType.isRangeFrom] get treated as a singleton
 * as per PartiQL specification.
 */
fun ExprValue.rangeOver(): Iterable<ExprValue> = when {
    type.isRangedFrom -> this
    // everything else ranges as a singleton unnamed value
    else -> listOf(this.unnamedValue())
}

/** A very simple string representation--to be used for diagnostic purposes only. */
fun ExprValue.stringify(): String =
    ConfigurableExprValueFormatter.standard.format(this)

val DEFAULT_COMPARATOR = NaturalExprValueComparators.NULLS_FIRST_ASC

/** Provides the default equality function. */
fun ExprValue.exprEquals(other: ExprValue): Boolean = DEFAULT_COMPARATOR.compare(this, other) == 0

/**
 * Provides the comparison predicate--which is not a total ordering.
 *
 * In particular, this operation will fail for non-comparable types.
 * For a total ordering over the PartiQL type space, see [NaturalExprValueComparators]
 */
operator fun ExprValue.compareTo(other: ExprValue): Int {
    return when {
        type.isUnknown || other.type.isUnknown ->
            throw EvaluationException("Null value cannot be compared: $this, $other", errorCode = ErrorCode.EVALUATOR_INVALID_COMPARISION, internal = false)
        isDirectlyComparableTo(other) -> DEFAULT_COMPARATOR.compare(this, other)
        else -> errNoContext("Cannot compare values: $this, $other", errorCode = ErrorCode.EVALUATOR_INVALID_COMPARISION, internal = false)
    }
}

/**
 * Checks if the two ExprValues are directly comparable.
 * Directly comparable is used in the context of the `<`/`<=`/`>`/`>=` operators.
 */
internal fun ExprValue.isDirectlyComparableTo(other: ExprValue): Boolean =
    when {
        // The ExprValue type for TIME and TIME WITH TIME ZONE is same
        // and thus needs to be checked explicitly for the timezone values.
        type == ExprValueType.TIME && other.type == ExprValueType.TIME ->
            timeValue().isDirectlyComparableTo(other.timeValue())
        else -> type.isDirectlyComparableTo(other.type)
    }

/** Regex to match DATE strings of the format yyyy-MM-dd */

/**
 * Casts this [ExprValue] to the target type.
 *
 * `MISSING` and `NULL` always convert to themselves no matter the target type.  When the
 * source type and target type are the same, this operation is a no-op.
 *
 * The conversion *to* a particular type is as follows, any conversion not specified raises
 * an [EvaluationException]:
 *
 *  * `BOOL`
 *      * Number types will convert to `false` if numerically equal to zero, `true` otherwise.
 *      * Text types will convert to `true` if case-insensitive text is `"true"`,
 *      convert to `false` if case-insensitive text is `"true"` and throw an error otherwise.
 *  * `INT`, `FLOAT`, and `DECIMAL`
 *      * `BOOL` converts as `1` for `true` and `0` for `false`
 *      * Number types will narrow or widen from the source type.  Narrowing is a truncation
 *      * Text types will convert using base-10 integral notation
 *          * For `FLOAT` and `DECIMAL` targets, decimal and e-notation is also supported.
 *  * `TIMESTAMP`
 *      * Text types will convert using the Ion text notation for timestamp (W3C/ISO-8601).
 *  * `DATE`
 *      * `TIMESTAMP` converts as `DATE` throwing away the additional information such as time.
 *      * Text types converts as `DATE` if the case-insensitive text is a valid ISO 8601 format date string.
 *  * `TIME`
 *      * `TIMESTAMP` converts as `TIME` throwing away the additional information such as date and time zone.
 *      * Text types converts as `TIME` if the case-insensitive text is a valid ISO 8601 format time string.
 *      * `TIME` and `TIME WITH TIME ZONE` converts as `TIME` throwing away the time zone information.
 *  * `TIME WITH TIME ZONE`
 *      * `TIMESTAMP` converts as `TIME WITH TIME ZONE` only if the timezone is defined in the TIMESTAMP value.
 *      The conversion throws away the additional information such as date.
 *      * Text types converts as `TIME WITH TIME ZONE` if the case-insensitive text is a valid ISO 8601 format time string.
 *      If the time zone is not specified, then the default time zone is used.
 *      * `TIME` and `TIME WITH TIME ZONE` converts as `TIME WITH TIME ZONE`.
 *      If the time zone is not specified, then the default time zone is used.
 *  * `STRING` and `SYMBOL`
 *      * `BOOL` converts to `STRING` as `"true"` and `"false"`;
 *        converts to `SYMBOL` as `'true'` and `'false'`.
 *      * Number types convert to decimal form with optional e-notation.
 *      * `TIMESTAMP` converts to the ISO-8601 format.
 *  * `BLOB` and `CLOB` can only convert between each other directly.
 *  * `LIST` and `SEXP`
 *      * Convert directly between each other.
 *      * `BAG` converts with an *arbitrary* order.
 *  * `STRUCT` only supports casting from itself.
 *  * `BAG` converts from `LIST` and `SEXP` by drops order guarantees.
 *
 * Note that *text types* is defined by [ExprValueType.isText], *number types* is defined by
 * [ExprValueType.isNumber], and *LOB types* is defined by [ExprValueType.isLob]
 *
 * @param targetType The target type to cast this value to.
 * @param valueFactory The ExprValueFactory used to create ExprValues.
 * @param typedOpBehavior TypedOpBehavior indicating how CAST should behave.
 * @param locationMeta The source location for the CAST. Used for error reporting.
 * specify the time zone.
 */
fun ExprValue.cast(
    targetType: SingleType,
    valueFactory: ExprValueFactory,
    locationMeta: SourceLocationMeta?,
    scalarTypeSystem: ScalarTypeSystem
): ExprValue {
    fun castExceptionContext(): PropertyValueMap {
        val errorContext = PropertyValueMap().also {
            it[Property.CAST_FROM] = this.type.toString()
            it[Property.CAST_TO] = targetType.runtimeType.toString()
        }

        locationMeta?.let { fillErrorContext(errorContext, it) }

        return errorContext
    }

    if (type.isUnknown) {
        return when (targetType) {
            is MissingType -> valueFactory.missingValue
            is NullType -> valueFactory.nullValue
            else -> this
        }
    }
    if ((targetType is CollectionType || targetType is StructType) && type == targetType.runtimeType) {
        return this
    }
    when (targetType) {
        is StaticScalarType -> scalarTypeSystem.invokeCastOp(this, targetType.toCompileTimeType(), locationMeta).let {
            if (it != null) { return it }
        }
        is ListType -> if (type.isSequence) return valueFactory.newList(asSequence())
        is SexpType -> if (type.isSequence) return valueFactory.newSexp(asSequence())
        is BagType -> if (type.isSequence) return valueFactory.newBag(asSequence())
        // no support for anything else
        else -> {}
    }

    val errorCode = if (locationMeta == null) {
        ErrorCode.EVALUATOR_INVALID_CAST_NO_LOCATION
    } else {
        ErrorCode.EVALUATOR_INVALID_CAST
    }

    // incompatible types
    err("Cannot convert $type to $targetType", errorCode, castExceptionContext(), internal = false)
}

/**
 * An Unknown value is one of `MISSING` or `NULL`
 */
internal fun ExprValue.isUnknown(): Boolean = this.type.isUnknown
/**
 * The opposite of [isUnknown].
 */
internal fun ExprValue.isNotUnknown(): Boolean = !this.type.isUnknown

/**
 * Creates a filter for unique ExprValues consistent with exprEquals. This filter is stateful keeping track of
 * seen [ExprValue]s.
 *
 * This filter is **stateful**!
 *
 * @return false if the value was seen before
 */
internal fun createUniqueExprValueFilter(): (ExprValue) -> Boolean {
    val seen = TreeSet<ExprValue>(DEFAULT_COMPARATOR)

    return { exprValue -> seen.add(exprValue) }
}
