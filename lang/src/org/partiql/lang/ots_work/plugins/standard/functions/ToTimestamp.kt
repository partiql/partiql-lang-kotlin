package org.partiql.lang.ots_work.plugins.standard.functions

import com.amazon.ion.Timestamp
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.functions.timestamp.FormatPattern
import org.partiql.lang.ots_work.plugins.standard.functions.timestamp.TimestampField
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.stringValue
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor

/**
 * PartiQL function to convert a formatted string into an Ion Timestamp.
 */
object ToTimestamp : ScalarFunction {
    override val signature = FunctionSignature(
        name = "to_timestamp",
        requiredParameters = listOf(listOf(StringType)),
        optionalParameter = listOf(StringType),
        returnType = listOf(TimeStampType)
    )

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
        val ts = try {
            Timestamp.valueOf(required[0].ionValue.stringValue())
        } catch (ex: IllegalArgumentException) {
            throw EvaluationException(
                message = "Timestamp was not a valid ion timestamp",
                errorCode = ErrorCode.EVALUATOR_ION_TIMESTAMP_PARSE_FAILURE,
                errorContext = PropertyValueMap(),
                cause = ex,
                internal = false
            )
        }
        return valueFactory.newTimestamp(ts)
    }

    override fun callWithOptional(required: List<ExprValue>, optional: ExprValue): ExprValue {
        val ts = TimestampParser.parseTimestamp(required[0].ionValue.stringValue()!!, optional.ionValue.stringValue()!!)
        return valueFactory.newTimestamp(ts)
    }
}

/**
 * Uses Java 8's DateTimeFormatter to parse an Ion Timestamp value.
 *
 * Note:  this is effected by https://bugs.openjdk.java.net/browse/JDK-8066806 which is not fixed until JDK-9.
 *
 * There are a few differences between Ion's timestamp and the {@ref java.time} package that create a few caveats
 * that we hope will be encountered very infrequently.
 *
 *  - The Ion specification allows for explicitly signifying of an unknown timestamp offset with a negative zero offset
 *    (i.e. the "-00:00" at the end of "2007-02-23T20:14:33.079-00:00") but Java 8's DateTimeFormatter simply doesn't
 *    recognize this and there's no reliable workaround that we've yet been able to determine.  Unfortunately, this
 *    means that unknown offsets specified are parsed as if they were explicitly UTC (i.e. "+00:00" or "Z").
 *  - DateTimeFormatter is capable of parsing UTC offsets to the precision of seconds, but Ion Timestamp's precision
 *    for offsets is 1 minute.  [TimestampParser] currently handles this by throwing an exception when an attempt
 *    is made to parse a timestamp with an offset that does does not land on a minute boundary.
 *  - Ion Java's Timestamp allows specification of offsets up to +/- 24h, while an exception is thrown by
 *    DateTimeFormatter for any attempt to parse an offset greater than +/- 18h.  The Ion specification does not seem
 *    to indicate minimum and maximum allowable values for offsets.  In practice this may not be an issue for systems
 *    that use Timestamps correctly because real-life offsets do not exceed +/- 12h.
 */
internal class TimestampParser {

    companion object {
        val TWO_DIGIT_PIVOT_YEAR = 70

        /** Converts the offset seconds value returned from the TemporalAccessor into the minutes value.
         * @throws EvaluationException if the offset seconds value was not a multiple of 60.
         */
        private fun TemporalAccessor.getLocalOffset(): Int? =
            if (!this.isSupported(ChronoField.OFFSET_SECONDS))
                null
            else {
                val offsetSeconds = this.get(ChronoField.OFFSET_SECONDS)
                if (offsetSeconds % 60 != 0) {
                    throw EvaluationException(
                        "The parsed timestamp has a UTC offset that not a multiple of 1 minute. " +
                            "This timestamp cannot be parsed accurately because the maximum " +
                            "resolution for an Ion timestamp offset is 1 minute.",
                        ErrorCode.EVALUATOR_PRECISION_LOSS_WHEN_PARSING_TIMESTAMP,
                        internal = false
                    )
                }
                offsetSeconds / 60
            }

        /**
         * Parses a string given the specified format pattern.
         */
        fun parseTimestamp(timestampString: String, formatPattern: String): Timestamp {
            val pattern = FormatPattern.fromString(formatPattern)
            // TODO: do this during compilation
            pattern.validateForTimestampParsing()

            val accessor: TemporalAccessor by lazy {
                try {
                    DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern(pattern.formatPatternString)
                        .toFormatter()
                        .parse(timestampString)

                    // DateTimeFormatter.ofPattern(formatPattern).parse(timestampString)
                } catch (ex: IllegalArgumentException) {
                    throw EvaluationException(
                        ex, ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
                        internal = false
                    )
                }
            }
            val year: Int by lazy {
                val year = accessor.get(ChronoField.YEAR)
                when {
                    !pattern.has2DigitYear || year < TWO_DIGIT_PIVOT_YEAR + 2000 -> year
                    else -> year - 100
                }
            }

            return try {
                when (pattern.leastSignificantField) {
                    TimestampField.FRACTION_OF_SECOND -> {
                        val nanoSeconds = BigDecimal.valueOf(accessor.getLong(ChronoField.NANO_OF_SECOND))
                        val secondsFraction = nanoSeconds.scaleByPowerOfTen(-9).stripTrailingZeros()
                        // Note that this overload of Timestamp.forSecond(...) creates a timestamp with "fraction" precision.
                        Timestamp.forSecond(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            accessor.get(ChronoField.HOUR_OF_DAY),
                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                            BigDecimal.valueOf(accessor.getLong(ChronoField.SECOND_OF_MINUTE)).add(
                                secondsFraction
                            ) as BigDecimal,
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.SECOND_OF_MINUTE -> {
                        // Note that this overload of Timestamp.forSecond(...) creates a timestamp with "second" precision.
                        Timestamp.forSecond(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            accessor.get(ChronoField.HOUR_OF_DAY),
                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                            accessor.get(ChronoField.SECOND_OF_MINUTE),
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.MINUTE_OF_HOUR -> {
                        Timestamp.forMinute(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            accessor.get(ChronoField.HOUR_OF_DAY),
                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.HOUR_OF_DAY -> {
                        Timestamp.forMinute(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            accessor.get(ChronoField.HOUR_OF_DAY),
                            0, // Ion Timestamp has no HOUR precision -- default minutes to 0
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.DAY_OF_MONTH -> {
                        Timestamp.forDay(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH)
                        )
                    }
                    TimestampField.MONTH_OF_YEAR -> {
                        Timestamp.forMonth(year, accessor.get(ChronoField.MONTH_OF_YEAR))
                    }
                    TimestampField.YEAR -> {
                        Timestamp.forYear(year)
                    }
                    TimestampField.AM_PM, TimestampField.OFFSET, null -> {
                        errNoContext(
                            "This code should be unreachable because AM_PM or OFFSET or null" +
                                "should never the value of formatPattern.leastSignificantField by at this point",
                            errorCode = ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
                            internal = true
                        )
                    }
                }
            }
            // Can be thrown by Timestamp.for*(...) methods.
            catch (ex: IllegalArgumentException) {
                throw EvaluationException(
                    ex,
                    ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
                    propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to formatPattern),
                    internal = false
                )
            }
            // Can be thrown by TemporalAccessor.get(ChronoField)
            catch (ex: DateTimeException) {
                throw EvaluationException(
                    ex,
                    ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
                    propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to formatPattern),
                    internal = false
                )
            }
        }
    }
}
