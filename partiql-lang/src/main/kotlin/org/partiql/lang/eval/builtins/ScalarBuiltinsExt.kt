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

package org.partiql.lang.eval.builtins

import org.partiql.lang.datetime.DateTimeUtils.parseTimestamp
import org.partiql.lang.datetime.FormatPattern
import org.partiql.lang.datetime.TimestampParser
import org.partiql.lang.datetime.TimestampTemporalAccessor
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.DEFAULT_COMPARATOR
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValue.Companion.newTimestamp
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.bigDecimalValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.longValue
import org.partiql.lang.eval.partiQLTimestampValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.SECONDS_PER_HOUR
import org.partiql.lang.eval.time.SECONDS_PER_MINUTE
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.syntax.impl.DateTimePart
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.TreeSet

/**
 * TODO replace this internal value once we have function libraries
 */
internal val SCALAR_BUILTINS_EXT = listOf(
    ExprFunctionExists,
    ExprFunctionUtcNow,
    ExprFunctionFilterDistinct,
    ExprFunctionDateAdd,
    ExprFunctionDateDiff,
    ExprFunctionMakeDate,
    ExprFunctionMakeTime,
    ExprFunctionToTimestamp,
    ExprFunctionSize,
    ExprFunctionFromUnix,
    ExprFunctionUnixTimestamp,
    ExprFunctionToString,
    ExprFunctionTextReplace,
)

/**
 * Given a PartiQL value returns true if and only if the value is a non-empty container(bag, sexp, list or struct),
 * returns false otherwise
 */
internal object ExprFunctionExists : ExprFunction {

    override val signature = FunctionSignature(
        name = "exists",
        requiredParameters = listOf(unionOf(StaticType.SEXP, StaticType.LIST, StaticType.BAG, StaticType.STRUCT)),
        returnType = StaticType.BOOL,
        unknownArguments = UnknownArguments.PASS_THRU
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val collection = required[0].asSequence()
        val result = collection.any()
        return ExprValue.newBoolean(result)
    }
}

/**
 * Returns the current time in UTC as a timestamp.
 */
internal object ExprFunctionUtcNow : ExprFunction {

    override val signature = FunctionSignature(
        name = "utcnow",
        requiredParameters = listOf(),
        returnType = StaticType.TIMESTAMP
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        return newTimestamp(session.now)
    }
}

/**
 * Returns a bag of distinct values contained within a bag, list, sexp, or struct.
 * If the container is a struct, the field names are not considered.
 */
internal object ExprFunctionFilterDistinct : ExprFunction {

    override val signature = FunctionSignature(
        name = "filter_distinct",
        requiredParameters = listOf(unionOf(StaticType.BAG, StaticType.LIST, StaticType.SEXP, StaticType.STRUCT)),
        returnType = StaticType.BAG
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val argument = required.first()
        // We cannot use a [HashSet] here because [ExprValue] does not implement .equals() and .hashCode()
        val encountered = TreeSet(DEFAULT_COMPARATOR)
        return ExprValue.newBag(
            sequence {
                argument.asSequence().forEach {
                    if (!encountered.contains(it)) {
                        encountered.add(it.unnamedValue())
                        yield(it)
                    }
                }
            }
        )
    }
}

/**
 * Given a data part, a quantity and a timestamp, returns an updated timestamp by altering datetime part by quantity
 *
 * Where DateTimePart is one of
 * * year
 * * month
 * * day
 * * hour
 * * minute
 * * decimalSecond
 */
internal object ExprFunctionDateAdd : ExprFunction {

    override val signature = FunctionSignature(
        name = "date_add",
        requiredParameters = listOf(StaticType.SYMBOL, unionOf(StaticType.INT, StaticType.DECIMAL), StaticType.TIMESTAMP),
        returnType = StaticType.TIMESTAMP
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val arg0 = required[0].stringValue()
        val part = DateTimePart.safeValueOf(arg0)
        val quantity = required[1]
        val timestamp = required[2].partiQLTimestampValue()
        val typeMismatchError: (field: String, quantity: ExprValue) -> Nothing = { f, q ->
            err(
                message = "Invalid $f value for date_add",
                errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                errorContext = propertyValueMapOf(
                    Property.EXPECTED_ARGUMENT_TYPES to "INT",
                    Property.FUNCTION_NAME to "date_add",
                    Property.ACTUAL_ARGUMENT_TYPES to q.type.name
                ),
                internal = false
            )
        }
        // TODO add a function lowering pass
        return try {
            val result = when (part) {
                DateTimePart.YEAR -> {
                    if (quantity.type == ExprValueType.INT) {
                        timestamp.plusYear(quantity.longValue())
                    } else {
                        typeMismatchError(part.name, quantity)
                    }
                }
                DateTimePart.MONTH -> {
                    if (quantity.type == ExprValueType.INT) {
                        timestamp.plusMonths(quantity.longValue())
                    } else {
                        typeMismatchError(part.name, quantity)
                    }
                }
                DateTimePart.DAY -> {
                    if (quantity.type == ExprValueType.INT) {
                        timestamp.plusDays(quantity.longValue())
                    } else {
                        typeMismatchError(part.name, quantity)
                    }
                }
                DateTimePart.HOUR -> {
                    if (quantity.type == ExprValueType.INT) {
                        timestamp.plusHours(quantity.longValue())
                    } else {
                        typeMismatchError(part.name, quantity)
                    }
                }
                DateTimePart.MINUTE -> {
                    if (quantity.type == ExprValueType.INT) {
                        timestamp.plusMinutes(quantity.longValue())
                    } else {
                        typeMismatchError(part.name, quantity)
                    }
                }
                DateTimePart.SECOND -> timestamp.plusSeconds(quantity.bigDecimalValue())
                else -> errNoContext(
                    "invalid datetime part for date_add: $arg0",
                    errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART,
                    internal = false
                )
            }
            newTimestamp(result)
        } catch (e: org.partiql.value.datetime.DateTimeException) {
            // IllegalArgumentExcept is thrown when the resulting timestamp go out of supported timestamp boundaries
            throw EvaluationException(e, errorCode = ErrorCode.EVALUATOR_TIMESTAMP_OUT_OF_BOUNDS, internal = false)
        }
    }
}

/**
 * Difference in datetime parts between two timestamps. If the first timestamp is later than the decimalSecond the result is negative.
 *
 * Syntax: `DATE_DIFF(<datetime part>, <timestamp>, <timestamp>)`
 * Where date time part is one of the following keywords: `year, month, day, hour, minute, decimalSecond`
 *
 * Timestamps without all datetime parts are considered to be in the beginning of the missing parts to make calculation possible.
 * For example:
 * - 2010T is interpreted as 2010-01-01T00:00:00.000Z
 * - date_diff(month, `2010T`, `2010-05T`) results in 4
 *
 * If one of the timestamps has a time component then they are a day apart only if they are 24h apart, examples:
 * - date_diff(day, `2010-01-01T`, `2010-01-02T`) results in 1
 * - date_diff(day, `2010-01-01T23:00Z`, `2010-01-02T01:00Z`) results in 0 as they are only 2h apart
 */
internal object ExprFunctionDateDiff : ExprFunction {

    override val signature = FunctionSignature(
        name = "date_diff",
        requiredParameters = listOf(StaticType.SYMBOL, StaticType.TIMESTAMP, StaticType.TIMESTAMP),
        returnType = unionOf(StaticType.INT, StaticType.DECIMAL)
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val arg0 = required[0].stringValue()
        val part = DateTimePart.safeValueOf(arg0)
        val l = required[1].partiQLTimestampValue().let {
            when (it) {
                is TimestampWithTimeZone -> it.atTimeZone(TimeZone.UtcOffset.of(0))
                is TimestampWithoutTimeZone -> it.withTimeZone(session.timeZone).atTimeZone(TimeZone.UtcOffset.of(0))
            }
        }
        val r = required[2].partiQLTimestampValue().let {
            when (it) {
                is TimestampWithTimeZone -> it.atTimeZone(TimeZone.UtcOffset.of(0))
                is TimestampWithoutTimeZone -> it.withTimeZone(session.timeZone).atTimeZone(TimeZone.UtcOffset.of(0))
            }
        }
        return when (part) {
            DateTimePart.YEAR -> Period.between(l.toDate().toLocalDate(), r.toDate().toLocalDate()).years.let { ExprValue.newInt(it) }
            DateTimePart.MONTH -> Period.between(l.toDate().toLocalDate(), r.toDate().toLocalDate()).toTotalMonths().let { ExprValue.newInt(it) }
            DateTimePart.DAY -> (r.epochSecond.minus(l.epochSecond).setScale(0, RoundingMode.DOWN).intValueExact() / (SECONDS_PER_HOUR * 24L)).let { ExprValue.newInt(it) }
            DateTimePart.HOUR -> (r.epochSecond.minus(l.epochSecond).setScale(0, RoundingMode.DOWN).intValueExact() / SECONDS_PER_HOUR.toLong()).let { ExprValue.newInt(it) }
            DateTimePart.MINUTE -> (r.epochSecond.minus(l.epochSecond).setScale(0, RoundingMode.DOWN).intValueExact() / SECONDS_PER_MINUTE.toLong()).let { ExprValue.newInt(it) }
            DateTimePart.SECOND -> (r.epochSecond.minus(l.epochSecond)).let { ExprValue.newDecimal(it) }
            else -> errNoContext(
                "invalid datetime part for date_diff: $arg0",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART,
                internal = false
            )
        }
    }
    private fun Date.toLocalDate() = LocalDate.of(this.year, this.month, this.day)
}

/**
 * Creates a DATE ExprValue from the date fields year, month and day.
 * Takes year, month and day as integers and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_date(<year_value>, <month_value>, <day_value>)
 */
internal object ExprFunctionMakeDate : ExprFunction {

    override val signature = FunctionSignature(
        name = "make_date",
        requiredParameters = listOf(StaticType.INT, StaticType.INT, StaticType.INT),
        returnType = StaticType.DATE
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val (year, month, day) = required.map {
            // TODO this should be handled by the signature validation, keeping it now as to not change any behavior
            if (it.type != ExprValueType.INT) {
                err(
                    message = "Invalid argument type for make_date",
                    errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                    errorContext = propertyValueMapOf(
                        Property.EXPECTED_ARGUMENT_TYPES to "INT",
                        Property.FUNCTION_NAME to "make_date",
                        Property.ACTUAL_ARGUMENT_TYPES to it.type.name
                    ),
                    internal = false
                )
            }
            it.intValue()
        }
        return try {
            ExprValue.newDate(year, month, day)
        } catch (e: DateTimeException) {
            err(
                message = "Date field value out of range. $year-$month-$day",
                errorCode = ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
                errorContext = propertyValueMapOf(),
                internal = false
            )
        }
    }
}

/**
 * Creates a TIME ExprValue from the time fields hour, minute, decimalSecond and optional timezone_minutes.
 * Takes hour, minute and optional timezone_minutes as integers, decimalSecond as decimal and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_time(<hour_value>, <minute_value>, <second_value>, <optional_timezone_minutes>?)
 */
internal object ExprFunctionMakeTime : ExprFunction {

    override val signature = FunctionSignature(
        name = "make_time",
        requiredParameters = listOf(StaticType.INT, StaticType.INT, StaticType.DECIMAL),
        optionalParameter = StaticType.INT,
        returnType = StaticType.TIME
    )

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val (hour, min, sec) = required
        return makeTime(hour.intValue(), min.intValue(), sec.bigDecimalValue(), opt.intValue())
    }

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val (hour, min, sec) = required
        return makeTime(hour.intValue(), min.intValue(), sec.bigDecimalValue(), null)
    }

    private fun makeTime(
        hour: Int,
        minute: Int,
        second: BigDecimal,
        tzMinutes: Int?
    ): ExprValue {
        try {
            return ExprValue.newTime(
                Time.of(
                    hour,
                    minute,
                    second.toInt(),
                    (second.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal())).toInt(),
                    second.scale(),
                    tzMinutes
                )
            )
        } catch (e: EvaluationException) {
            err(
                message = e.message,
                errorCode = ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE,
                errorContext = e.errorContext,
                internal = false
            )
        }
    }
}

/**
 * PartiQL function to convert a formatted string into a Timestamp.
 */
internal object ExprFunctionToTimestamp : ExprFunction {

    override val signature = FunctionSignature(
        name = "to_timestamp",
        requiredParameters = listOf(StaticType.STRING),
        optionalParameter = StaticType.STRING,
        returnType = StaticType.TIMESTAMP
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val ts = try {
            parseTimestamp(required[0].stringValue())
        } catch (ex: IllegalArgumentException) {
            throw EvaluationException(
                message = "Timestamp was not a valid timestamp",
                errorCode = ErrorCode.EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE,
                errorContext = PropertyValueMap(),
                cause = ex,
                internal = false
            )
        }
        return newTimestamp(ts)
    }

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val ts = TimestampParser.parseTimestamp(required[0].stringValue(), opt.stringValue())
        return newTimestamp(ts)
    }
}

/**
 * Builtin function to return the size of a container type, i.e. size of Lists, Structs and Bags. This function
 * propagates null and missing values as described in docs/Functions.md
 *
 * syntax: `size(<container>)` where container can be a BAG, SEXP, STRUCT or LIST.
 */
internal object ExprFunctionSize : ExprFunction {

    override val signature = FunctionSignature(
        name = "size",
        requiredParameters = listOf(unionOf(StaticType.LIST, StaticType.BAG, StaticType.STRUCT, StaticType.SEXP)),
        returnType = StaticType.INT
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val collection = required[0]
        val result = collection.count()
        return ExprValue.newInt(result)
    }
}

/**
 * Builtin function to convert the given unix epoch into a PartiQL `TIMESTAMP` [ExprValue]. A unix epoch represents
 * the seconds since '1970-01-01 00:00:00' UTC. Largely based off MySQL's FROM_UNIXTIME.
 *
 * Syntax: `FROM_UNIXTIME(unix_timestamp)`
 * Where unix_timestamp is a (potentially decimal) numeric value. If unix_timestamp is a decimal, the returned
 * `TIMESTAMP` will have fractional seconds. If unix_timestamp is an integer, the returned `TIMESTAMP` will not have
 * fractional seconds.
 *
 * When given a negative numeric value, this function returns a PartiQL `TIMESTAMP` [ExprValue] before the last epoch.
 * When given a non-negative numeric value, this function returns a PartiQL `TIMESTAMP` [ExprValue] after the last
 * epoch.
 */
internal object ExprFunctionFromUnix : ExprFunction {

    private val millisPerSecond = BigDecimal(1000)

    override val signature = FunctionSignature(
        name = "from_unixtime",
        requiredParameters = listOf(unionOf(StaticType.DECIMAL, StaticType.INT)),
        returnType = StaticType.TIMESTAMP
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val unixTimestamp = required[0].bigDecimalValue()
        return newTimestamp(DateTimeValue.timestamp(unixTimestamp, session.timeZone))
    }
}

/**
 * Builtin function to convert the given PartiQL `TIMESTAMP` [ExprValue] into a unix epoch, where a unix epoch
 * represents the seconds since '1970-01-01 00:00:00' UTC. Largely based off MySQL's UNIX_TIMESTAMP.
 *
 * Syntax: `UNIX_TIMESTAMP([timestamp])`
 *
 * If UNIX_TIMESTAMP() is called with no [timestamp] argument, it returns the number of seconds with millisecond precision
 * since '1970-01-01 00:00:00' UTC as a PartiQL `DECIMAL` [ExprValue]
 *
 * If UNIX_TIMESTAMP() is called with a [timestamp] argument, it returns the number of seconds from
 * '1970-01-01 00:00:00' UTC to the given [timestamp] argument. If given a [timestamp] before the last epoch, will
 * return the number of seconds before the last epoch as a negative number. The return value will be a decimal.
 *
 * The valid range of argument values is the range of PartiQL's `TIMESTAMP` value.
 *
 * Note: if the timestamp is a timestamp without time zone, it will be convert to a timestamp with time zone,
 * based on session time zone.
 */
internal object ExprFunctionUnixTimestamp : ExprFunction {

    override val signature = FunctionSignature(
        name = "unix_timestamp",
        requiredParameters = listOf(),
        optionalParameter = StaticType.TIMESTAMP,
        returnType = StaticType.DECIMAL
    )
    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        return ExprValue.newDecimal(session.nowZ.epochSecond)
    }

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val timestamp = opt.partiQLTimestampValue().let {
            when (it) {
                is TimestampWithTimeZone -> it
                is TimestampWithoutTimeZone -> it.withTimeZone(session.timeZone)
            }
        }

        return ExprValue.newDecimal(timestamp.epochSecond)
    }
}

/**
 * Given a timestamp and a format pattern return a string representation of the timestamp in the given format.
 *
 * Where TimeFormatPattern is a String with the following special character interpretations
 *
 * Note:
 * 1. If the timestamp is a timestamp without time zone, and the pattern indicate the string should contain timezone information
 *     the timestamp will be cast to timestamp with time zone using the session offset
 * 2. If the timestamp is a timestamp with unknown, and the pattern indicate the string should contain timezone information
 *     the timezone will be shown as **UTC**.
 * 3. If the timestamp is a timestamp with timezone, and the pattern indicate the string should not contain timezone information
 *     the timestamp will be cast to the timestamp without timezone, by converting to session local time.
 */
internal object ExprFunctionToString : ExprFunction {

    override val signature = FunctionSignature(
        name = "to_string",
        requiredParameters = listOf(StaticType.TIMESTAMP, StaticType.STRING),
        returnType = StaticType.STRING
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val pattern = required[1].stringValue()

        // Check if the input pattern contains only the allowed symbols
        val parsedPattern = FormatPattern.fromString(pattern)

        // timezone manipulation
        val timestamp = required[0].partiQLTimestampValue()
        val modifiedTs = when (parsedPattern.hasOffset) {
            true -> {
                when (timestamp) {
                    is TimestampWithTimeZone -> timestamp
                    is TimestampWithoutTimeZone -> timestamp.withTimeZone(session.timeZone)
                }
            }
            false -> {
                when (timestamp) {
                    is TimestampWithTimeZone -> timestamp
                    is TimestampWithoutTimeZone -> timestamp.withTimeZone(session.timeZone)
                }
            }
        }

        val formatter: DateTimeFormatter = try {
            DateTimeFormatter.ofPattern(pattern)
        } catch (ex: IllegalArgumentException) {
            errInvalidFormatPattern(pattern, ex)
        }

        val temporalAccessor = TimestampTemporalAccessor(modifiedTs)
        try {
            return ExprValue.newString(formatter.format(temporalAccessor))
        } catch (ex: UnsupportedTemporalTypeException) {
            errInvalidFormatPattern(pattern, ex)
        } catch (ex: DateTimeException) {
            errInvalidFormatPattern(pattern, ex)
        }
    }

    private fun errInvalidFormatPattern(pattern: String, cause: Exception): Nothing {
        val pvmap = PropertyValueMap()
        pvmap[Property.TIMESTAMP_FORMAT_PATTERN] = pattern
        throw EvaluationException(
            "Invalid DateTime format pattern",
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
            pvmap,
            cause,
            internal = false
        )
    }
}

/** text_replace(string, from, to) -- in [string], replaces each occurrence of [from] with [to].
 */
internal object ExprFunctionTextReplace : ExprFunction {
    override val signature = FunctionSignature(
        name = "text_replace",
        requiredParameters = listOf(StaticType.TEXT, StaticType.TEXT, StaticType.TEXT),
        returnType = StaticType.TEXT,
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val string = required[0].stringValue()
        val from = required[1].stringValue()
        val to = required[2].stringValue()
        return ExprValue.newString(string.replace(from, to))
    }
}
