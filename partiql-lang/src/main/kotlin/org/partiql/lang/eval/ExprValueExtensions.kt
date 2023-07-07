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

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.Time
import org.partiql.lang.syntax.impl.DateTimePart
import org.partiql.lang.types.StaticTypeUtils.getRuntimeType
import org.partiql.lang.util.ConfigurableExprValueFormatter
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.coerce
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.downcast
import org.partiql.lang.util.getPrecisionFromTimeString
import org.partiql.lang.util.isNaN
import org.partiql.lang.util.isNegInf
import org.partiql.lang.util.isPosInf
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
import org.partiql.types.SingleType
import org.partiql.types.StringType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.TreeMap
import java.util.TreeSet
import kotlin.math.round

const val MISSING_ANNOTATION = "\$missing"
const val BAG_ANNOTATION = "\$bag"
const val DATE_ANNOTATION = "\$date"
const val TIME_ANNOTATION = "\$time"
const val GRAPH_ANNOTATION = "\$graph"

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
    scalar.booleanValue() ?: errNoContext("Expected boolean: $this", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.numberValue(): Number =
    scalar.numberValue() ?: errNoContext("Expected number: $this", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.dateValue(): LocalDate =
    scalar.dateValue() ?: errNoContext("Expected date: $this", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.timeValue(): Time =
    scalar.timeValue() ?: errNoContext("Expected time: $this", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.timestampValue(): Timestamp =
    scalar.timestampValue() ?: errNoContext("Expected timestamp: $this", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.stringValue(): String =
    scalar.stringValue() ?: errNoContext("Expected string: $this", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

fun ExprValue.bytesValue(): ByteArray =
    scalar.bytesValue() ?: errNoContext("Expected byte array: $this", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE_TYPE, internal = false)

internal fun ExprValue.dateTimePartValue(): DateTimePart =
    try {
        DateTimePart.valueOf(this.stringValue().toUpperCase())
    } catch (e: IllegalArgumentException) {
        throw EvaluationException(
            cause = e,
            message = "invalid datetime part, valid values: [${DateTimePart.values().joinToString()}]",
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

/** Types that are cast to the [ExprValueType.isText] types by calling `IonValue.toString()`. */
private val ION_TEXT_STRING_CAST_TYPES = setOf(ExprValueType.BOOL, ExprValueType.TIMESTAMP)

/** Regex to match DATE strings of the format yyyy-MM-dd */
private val datePatternRegex = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")

private val genericTimeRegex = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?([+|-]\\d\\d:\\d\\d)?")

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
 * @param defaultTimezoneOffset Default timezone offset to be used when TIME WITH TIME ZONE does not explicitly
 * specify the time zone.
 */
fun ExprValue.cast(
    targetType: SingleType,
    typedOpBehavior: TypedOpBehavior,
    locationMeta: SourceLocationMeta?,
    defaultTimezoneOffset: ZoneOffset
): ExprValue {
    fun castExceptionContext(): PropertyValueMap {
        val errorContext = PropertyValueMap().also {
            it[Property.CAST_FROM] = this.type.toString()
            it[Property.CAST_TO] = getRuntimeType(targetType).toString()
        }

        locationMeta?.let { fillErrorContext(errorContext, it) }

        return errorContext
    }

    fun castFailedErr(message: String, internal: Boolean, cause: Throwable? = null): Nothing {
        val errorContext = castExceptionContext()

        val errorCode = if (locationMeta == null) {
            ErrorCode.EVALUATOR_CAST_FAILED_NO_LOCATION
        } else {
            ErrorCode.EVALUATOR_CAST_FAILED
        }

        throw EvaluationException(
            message = message,
            errorCode = errorCode,
            errorContext = errorContext,
            internal = internal,
            cause = cause
        )
    }

    val longMaxDecimal = bigDecimalOf(Long.MAX_VALUE)
    val longMinDecimal = bigDecimalOf(Long.MIN_VALUE)

    fun Number.exprValue(type: SingleType) = when (type) {
        is IntType -> {
            // If the source is Positive/Negative Infinity or Nan, We throw an error
            if (this.isNaN || this.isNegInf || this.isPosInf) {
                castFailedErr("Can't convert Infinity or NaN to INT.", internal = false)
            }

            val rangeForType = when (typedOpBehavior) {
                TypedOpBehavior.HONOR_PARAMETERS ->
                    when (type.rangeConstraint) {
                        // There is not CAST syntax to that can execute this branch today.
                        IntType.IntRangeConstraint.SHORT -> LongRange(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong())
                        IntType.IntRangeConstraint.INT4 -> LongRange(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
                        IntType.IntRangeConstraint.LONG, IntType.IntRangeConstraint.UNCONSTRAINED ->
                            LongRange(Long.MIN_VALUE, Long.MAX_VALUE)
                    }
            }

            // Here, we check if there is a possibility of being able to fit this number into
            // any of the integer types. We allow the buffer of 1 because we allow rounding into min/max values.
            if (this <= (longMinDecimal - BigDecimal.ONE) || this >= (longMaxDecimal + BigDecimal.ONE)) {
                errIntOverflow(8)
            }

            // We round the value to the nearest integral value
            // In legacy behavior, this always picks the floor integer value
            // Else, rounding is done through https://en.wikipedia.org/wiki/Rounding#Round_half_to_even
            // We don't convert the result to Long within the when block here
            //  because the rounded values can still be out of range for Kotlin's Long.
            val result = when (typedOpBehavior) {
                TypedOpBehavior.HONOR_PARAMETERS -> when (this) {
                    is BigDecimal -> this.setScale(0, RoundingMode.HALF_EVEN)
                    // [kotlin.math.round] rounds towards the closes even number on tie
                    //   https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.math/round.html
                    is Float -> round(this)
                    is Double -> round(this)
                    else -> this
                }
            }.let {
                // after rounding, check that the value can fit into range of the type being casted into
                if (it < rangeForType.first || it > rangeForType.last) {
                    errIntOverflow(8)
                }
                it.toLong()
            }
            ExprValue.newInt(result)
        }
        is FloatType -> ExprValue.newFloat(this.toDouble())
        is DecimalType -> {
            if (this.isNaN || this.isNegInf || this.isPosInf) {
                castFailedErr("Can't convert Infinity or NaN to DECIMAL.", internal = false)
            }

            when (typedOpBehavior) {
                TypedOpBehavior.HONOR_PARAMETERS ->
                    when (val constraint = type.precisionScaleConstraint) {
                        DecimalType.PrecisionScaleConstraint.Unconstrained -> ExprValue.newDecimal(this.coerce(BigDecimal::class.java))
                        is DecimalType.PrecisionScaleConstraint.Constrained -> {
                            val decimal = this.coerce(BigDecimal::class.java)
                            val result = decimal.round(MathContext(constraint.precision))
                                .setScale(constraint.scale, RoundingMode.HALF_UP)
                            if (result.precision() > constraint.precision) {
                                // Following PostgresSQL behavior here. Java will increase precision if needed.
                                castFailedErr("target type DECIMAL(${constraint.precision}, ${constraint.scale}) too small for value $decimal.", internal = false)
                            } else {
                                ExprValue.newDecimal(result)
                            }
                        }
                    }
            }
        }
        else -> castFailedErr("Invalid type for numeric conversion: $type (this code should be unreachable)", internal = true)
    }

    fun String.exprValue(type: SingleType) = when (type) {
        is StringType -> when (typedOpBehavior) {
            TypedOpBehavior.HONOR_PARAMETERS -> when (val constraint = type.lengthConstraint) {
                StringType.StringLengthConstraint.Unconstrained -> ExprValue.newString(this)
                is StringType.StringLengthConstraint.Constrained -> {
                    val actualCodepointCount = this.codePointCount(0, this.length)
                    val lengthConstraint = constraint.length.value
                    val truncatedString = if (actualCodepointCount <= lengthConstraint) {
                        this // no truncation needed
                    } else {
                        this.substring(0, this.offsetByCodePoints(0, lengthConstraint))
                    }

                    ExprValue.newString(
                        when (constraint.length) {
                            is NumberConstraint.Equals -> truncatedString.trimEnd { c -> c == '\u0020' }
                            is NumberConstraint.UpTo -> truncatedString
                        }
                    )
                }
            }
        }
        is SymbolType -> ExprValue.newSymbol(this)

        else -> castFailedErr("Invalid type for textual conversion: $type (this code should be unreachable)", internal = true)
    }

    when {
        type.isUnknown && targetType is MissingType -> return ExprValue.missingValue
        type.isUnknown && targetType is NullType -> return ExprValue.nullValue
        type.isUnknown -> return this
        // Note that the ExprValueType for TIME and TIME WITH TIME ZONE is the same i.e. ExprValueType.TIME.
        // We further need to check for the time zone and hence we do not short circuit here when the type is TIME.
        type == getRuntimeType(targetType) && type != ExprValueType.TIME -> {
            return when (targetType) {
                is IntType, is FloatType, is DecimalType -> numberValue().exprValue(targetType)
                is StringType -> stringValue().exprValue(targetType)
                else -> this
            }
        }
        else -> {
            when (targetType) {
                is BoolType -> when {
                    type.isNumber -> return when {
                        numberValue().compareTo(0L) == 0 -> ExprValue.newBoolean(false)
                        else -> ExprValue.newBoolean(true)
                    }
                    type.isText -> return when (stringValue().lowercase()) {
                        "true" -> ExprValue.newBoolean(true)
                        "false" -> ExprValue.newBoolean(false)
                        else -> castFailedErr("can't convert string value to BOOL", internal = false)
                    }
                }
                is IntType -> when {
                    type == ExprValueType.BOOL -> return if (booleanValue()) 1L.exprValue(targetType) else 0L.exprValue(targetType)
                    type.isNumber -> return numberValue().exprValue(targetType)
                    type.isText -> {
                        // Here, we use ion java library to help the transform from string to int
                        // TODO: have our own parser implemented and remove dependency on Ion, https://github.com/partiql/partiql-lang-kotlin/issues/956
                        fun parseToLong(s: String): Long {
                            val ion = IonSystemBuilder.standard().build()
                            val value = try {
                                val normalized = s.normalizeForCastToInt()
                                ion.singleValue(normalized) as IonInt
                            } catch (e: Exception) {
                                castFailedErr("can't convert string value to INT", internal = false, cause = e)
                            }
                            return when (value.integerSize) {
                                // Our numbers comparison machinery does not handle big integers yet, fail fast
                                IntegerSize.BIG_INTEGER -> errIntOverflow(8, errorContextFrom(locationMeta))
                                else -> value.longValue()
                            }
                        }

                        return parseToLong(stringValue()).exprValue(targetType)
                    }
                }
                is FloatType -> when {
                    type == ExprValueType.BOOL -> return if (booleanValue()) 1.0.exprValue(targetType) else 0.0.exprValue(targetType)
                    type.isNumber -> return numberValue().toDouble().exprValue(targetType)
                    type.isText ->
                        try {
                            return stringValue().toDouble().exprValue(targetType)
                        } catch (e: NumberFormatException) {
                            castFailedErr("can't convert string value to FLOAT", internal = false, cause = e)
                        }
                }
                is DecimalType -> when {
                    type == ExprValueType.BOOL -> return if (booleanValue()) {
                        BigDecimal.ONE.exprValue(targetType)
                    } else {
                        BigDecimal.ZERO.exprValue(targetType)
                    }
                    type.isNumber -> return numberValue().exprValue(targetType)
                    type.isText -> try {
                        return bigDecimalOf(stringValue()).exprValue(targetType)
                    } catch (e: NumberFormatException) {
                        castFailedErr("can't convert string value to DECIMAL", internal = false, cause = e)
                    }
                }
                is TimestampType -> when {
                    type.isText -> try {
                        return ExprValue.newTimestamp(Timestamp.valueOf(stringValue()))
                    } catch (e: IllegalArgumentException) {
                        castFailedErr("can't convert string value to TIMESTAMP", internal = false, cause = e)
                    }
                }
                is DateType -> when {
                    type == ExprValueType.TIMESTAMP -> {
                        val ts = timestampValue()
                        return ExprValue.newDate(LocalDate.of(ts.year, ts.month, ts.day))
                    }
                    type.isText -> try {
                        // validate that the date string follows the format YYYY-MM-DD
                        if (!datePatternRegex.matches(stringValue())) {
                            castFailedErr(
                                "Can't convert string value to DATE. Expected valid date string " +
                                    "and the date format to be YYYY-MM-DD",
                                internal = false
                            )
                        }
                        val date = LocalDate.parse(stringValue())
                        return ExprValue.newDate(date)
                    } catch (e: DateTimeParseException) {
                        castFailedErr(
                            "Can't convert string value to DATE. Expected valid date string " +
                                "and the date format to be YYYY-MM-DD",
                            internal = false, cause = e
                        )
                    }
                }
                is TimeType -> {
                    val precision = targetType.precision
                    when {
                        type == ExprValueType.TIME -> {
                            val time = timeValue()
                            val timeZoneOffset = when (targetType.withTimeZone) {
                                true -> time.zoneOffset ?: defaultTimezoneOffset
                                else -> null
                            }
                            return ExprValue.newTime(
                                Time.of(
                                    time.localTime,
                                    precision ?: time.precision,
                                    timeZoneOffset
                                )
                            )
                        }
                        type == ExprValueType.TIMESTAMP -> {
                            val ts = timestampValue()
                            val timeZoneOffset = when (targetType.withTimeZone) {
                                true -> ts.localOffset ?: castFailedErr(
                                    "Can't convert timestamp value with unknown local offset (i.e. -00:00) to TIME WITH TIME ZONE.",
                                    internal = false
                                )
                                else -> null
                            }
                            return ExprValue.newTime(
                                Time.of(
                                    ts.hour,
                                    ts.minute,
                                    ts.second,
                                    (ts.decimalSecond.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal())).toInt(),
                                    precision ?: ts.decimalSecond.scale(),
                                    timeZoneOffset
                                )
                            )
                        }
                        type.isText -> try {
                            // validate that the time string follows the format HH:MM:SS[.ddddd...][+|-HH:MM]
                            val matcher = genericTimeRegex.toPattern().matcher(stringValue())
                            if (!matcher.find()) {
                                castFailedErr(
                                    "Can't convert string value to TIME. Expected valid time string " +
                                        "and the time to be of the format HH:MM:SS[.ddddd...][+|-HH:MM]",
                                    internal = false
                                )
                            }

                            val localTime = LocalTime.parse(stringValue(), DateTimeFormatter.ISO_TIME)

                            // Note that the [genericTimeRegex] has a group to extract the zone offset.
                            val zoneOffsetString = matcher.group(2)
                            val zoneOffset = zoneOffsetString?.let { ZoneOffset.of(it) } ?: defaultTimezoneOffset

                            return ExprValue.newTime(
                                Time.of(
                                    localTime,
                                    precision ?: getPrecisionFromTimeString(stringValue()),
                                    when (targetType.withTimeZone) {
                                        true -> zoneOffset
                                        else -> null
                                    }
                                )
                            )
                        } catch (e: DateTimeParseException) {
                            castFailedErr(
                                "Can't convert string value to TIME. Expected valid time string " +
                                    "and the time format to be HH:MM:SS[.ddddd...][+|-HH:MM]",
                                internal = false, cause = e
                            )
                        }
                    }
                }
                is StringType, is SymbolType -> when {
                    type.isNumber -> return numberValue().toString().exprValue(targetType)
                    type.isText -> return stringValue().exprValue(targetType)
                    type == ExprValueType.DATE -> return dateValue().toString().exprValue(targetType)
                    type == ExprValueType.TIME -> return timeValue().toString().exprValue(targetType)
                    type == ExprValueType.BOOL -> return booleanValue().toString().exprValue(targetType)
                    type == ExprValueType.TIMESTAMP -> return timestampValue().toString().exprValue(targetType)
                }
                is ClobType -> when {
                    type.isLob -> return ExprValue.newClob(bytesValue())
                }
                is BlobType -> when {
                    type.isLob -> return ExprValue.newBlob(bytesValue())
                }
                is ListType -> if (type.isSequence) return ExprValue.newList(asSequence())
                is SexpType -> if (type.isSequence) return ExprValue.newSexp(asSequence())
                is BagType -> if (type.isSequence) return ExprValue.newBag(asSequence())
                // no support for anything else
                else -> {}
            }
        }
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
 * Remove leading spaces in decimal notation and the plus sign
 *
 * Examples:
 * - `"00001".normalizeForIntCast() == "1"`
 * - `"-00001".normalizeForIntCast() == "-1"`
 * - `"0x00001".normalizeForIntCast() == "0x00001"`
 * - `"+0x00001".normalizeForIntCast() == "0x00001"`
 * - `"000a".normalizeForIntCast() == "a"`
 */
private fun String.normalizeForCastToInt(): String {
    fun Char.isSign() = this == '-' || this == '+'
    fun Char.isHexOrBase2Marker(): Boolean {
        val c = this.lowercaseChar()

        return c == 'x' || c == 'b'
    }

    fun String.possiblyHexOrBase2() = (length >= 2 && this[1].isHexOrBase2Marker()) ||
        (length >= 3 && this[0].isSign() && this[2].isHexOrBase2Marker())

    return when {
        length == 0 -> this
        possiblyHexOrBase2() -> {
            if (this[0] == '+') {
                this.drop(1)
            } else {
                this
            }
        }
        else -> {
            val (isNegative, startIndex) = when (this[0]) {
                '-' -> Pair(true, 1)
                '+' -> Pair(false, 1)
                else -> Pair(false, 0)
            }

            var toDrop = startIndex
            while (toDrop < length && this[toDrop] == '0') {
                toDrop += 1
            }

            when {
                toDrop == length -> "0" // string is all zeros
                toDrop == 0 -> this
                toDrop == 1 && isNegative -> this
                toDrop > 1 && isNegative -> '-' + this.drop(toDrop)
                else -> this.drop(toDrop)
            }
        }
    }
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

fun Sequence<ExprValue>.distinct(): Sequence<ExprValue> {
    return sequence {
        val seen = TreeSet(DEFAULT_COMPARATOR)
        this@distinct.forEach {
            if (!seen.contains(it)) {
                seen.add(it.unnamedValue())
                yield(it)
            }
        }
    }
}

fun Sequence<ExprValue>.multiplicities(): TreeMap<ExprValue, Int> {
    val multiplicities: TreeMap<ExprValue, Int> = TreeMap(DEFAULT_COMPARATOR)
    this.forEach {
        multiplicities.compute(it) { _, v -> (v ?: 0) + 1 }
    }
    return multiplicities
}

/**
 * This method should only be used in case we want to get result from querying an Ion file or an [IonValue]
 */
fun ExprValue.toIonValue(ion: IonSystem): IonValue =
    when (type) {
        ExprValueType.NULL -> ion.newNull(asFacet(IonType::class.java))
        ExprValueType.MISSING -> ion.newNull().apply { addTypeAnnotation(MISSING_ANNOTATION) }
        ExprValueType.BOOL -> ion.newBool(booleanValue())
        ExprValueType.INT -> ion.newInt(longValue())
        ExprValueType.FLOAT -> ion.newFloat(numberValue().toDouble())
        ExprValueType.DECIMAL -> ion.newDecimal(bigDecimalValue())
        ExprValueType.DATE -> {
            val value = dateValue()
            ion.newTimestamp(Timestamp.forDay(value.year, value.monthValue, value.dayOfMonth)).apply {
                addTypeAnnotation(DATE_ANNOTATION)
            }
        }
        ExprValueType.TIMESTAMP -> ion.newTimestamp(timestampValue())
        ExprValueType.TIME -> timeValue().toIonValue(ion)
        ExprValueType.SYMBOL -> ion.newSymbol(stringValue())
        ExprValueType.STRING -> ion.newString(stringValue())
        ExprValueType.CLOB -> ion.newClob(bytesValue())
        ExprValueType.BLOB -> ion.newBlob(bytesValue())
        ExprValueType.LIST -> mapTo(ion.newEmptyList()) {
            if (it is StructExprValue)
                it.toIonStruct(ion)
            else
                it.toIonValue(ion).clone()
        }
        ExprValueType.SEXP -> mapTo(ion.newEmptySexp()) {
            if (it is StructExprValue)
                it.toIonStruct(ion)
            else
                it.toIonValue(ion).clone()
        }
        ExprValueType.BAG -> mapTo(
            ion.newEmptyList().apply { addTypeAnnotation(BAG_ANNOTATION) }
        ) {
            if (it is StructExprValue)
                it.toIonStruct(ion)
            else
                it.toIonValue(ion).clone()
        }
        ExprValueType.STRUCT -> toIonStruct(ion)
        ExprValueType.GRAPH -> TODO("Ion representation for graph values, maybe?")
    }

private fun ExprValue.toIonStruct(ion: IonSystem): IonStruct {
    return ion.newEmptyStruct().apply {
        this@toIonStruct.forEach {
            val nameVal = it.name
            if (nameVal != null && nameVal.type.isText && it.type != ExprValueType.MISSING) {
                val name = nameVal.stringValue()
                add(name, it.toIonValue(ion).clone())
            }
        }
    }
}
