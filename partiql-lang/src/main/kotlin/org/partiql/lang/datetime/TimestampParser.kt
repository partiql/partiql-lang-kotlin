package org.partiql.lang.datetime

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor

/**
 * Uses Java 8's DateTimeFormatter to parse an Ion Timestamp value.
 *
 * Note:  this is effected by https://bugs.openjdk.java.net/browse/JDK-8066806 which is not fixed until JDK-9.
 *
 * There are a few differences between PartiQL's timestamp and the {@ref java.time} package that create a few caveats
 * that we hope will be encountered very infrequently.
 *
 *  - PartiQL Timestamp allows for explicitly signifying of an unknown timestamp offset with a negative zero offset
 *    (i.e. the "-00:00" at the end of "2007-02-23T20:14:33.079-00:00") but Java 8's DateTimeFormatter simply doesn't
 *    recognize this and there's no reliable workaround that we've yet been able to determine.  Unfortunately, this
 *    means that unknown offsets specified are parsed as if they were explicitly UTC (i.e. "+00:00" or "Z").
 *    It is worth to note that, if a timestamp string has no indication of timezone/offset, it is possible that to assign
 *    unknown offset (-00:00) to the resutling timestamp, and it is probably the correct thing to do.
 *    However, doing so creates a confusing situation which an unknown timestamp may or may not be created by this parser
 *    depending on the input string. Hence, we make the choice that this implementation will always produce UTC offset(+00:00)
 *    in case of explicit or implicit unknown timestamp.
 *  - DateTimeFormatter is capable of parsing UTC offsets to the precision of seconds, but PartiQL (as well as SQL spec)
 *    specifies Timestamp's precision to the minute precision.
 *    [TimestampParser] currently handles this by throwing an exception when an attempt is made to parse a timestamp
 *    with an offset that does does not land on a minute boundary.
 *  - PartiQL's Timestamp allows specification of offsets up to +/- 23:59, while an exception is thrown by
 *    DateTimeFormatter for any attempt to parse an offset greater than +/- 18h. In practice this may not be an issue for systems
 *    that use Timestamps correctly because real-life offsets do not exceed +/- 12h.
 *
 * This class should only be used by [org.partiql.lang.eval.builtins.ExprFunctionToTimestamp]
 * as an extension to supported format parsing.
 */
internal class TimestampParser {

    companion object {
        val TWO_DIGIT_PIVOT_YEAR = 70

        /** Converts the offset seconds value returned from the TemporalAccessor into the minutes value.
         * @throws EvaluationException if the offset seconds value was not a multiple of 60.
         */
        private fun TemporalAccessor.getLocalOffset(): TimeZone? =
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
                TimeZone.UtcOffset.of(offsetSeconds / 60)
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
                        Timestamp.of(
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
                        Timestamp.of(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            accessor.get(ChronoField.HOUR_OF_DAY),
                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                            BigDecimal.valueOf(accessor.get(ChronoField.SECOND_OF_MINUTE).toLong()),
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.MINUTE_OF_HOUR -> {
                        Timestamp.of(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            accessor.get(ChronoField.HOUR_OF_DAY),
                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                            BigDecimal.ZERO,
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.HOUR_OF_DAY -> {
                        Timestamp.of(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            accessor.get(ChronoField.HOUR_OF_DAY),
                            0,
                            BigDecimal.ZERO, // Ion Timestamp has no HOUR precision -- default minutes to 0
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.DAY_OF_MONTH -> {
                        Timestamp.of(
                            year,
                            accessor.get(ChronoField.MONTH_OF_YEAR),
                            accessor.get(ChronoField.DAY_OF_MONTH),
                            0, 0, BigDecimal.ZERO,
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.MONTH_OF_YEAR -> {
                        Timestamp.of(
                            year, accessor.get(ChronoField.MONTH_OF_YEAR), 1,
                            0, 0, BigDecimal.ZERO,
                            accessor.getLocalOffset()
                        )
                    }
                    TimestampField.YEAR -> {
                        Timestamp.of(
                            year, 1, 1,
                            0, 0, BigDecimal.ZERO,
                            accessor.getLocalOffset()
                        )
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
            // Can be thrown by Timestamp.of methods.
            catch (ex: org.partiql.value.datetime.DateTimeException) {
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
