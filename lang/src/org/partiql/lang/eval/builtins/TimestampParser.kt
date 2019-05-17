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

import com.amazon.ion.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.partiql.lang.eval.builtins.timestamp.*
import org.partiql.lang.util.*
import java.math.*
import java.time.*
import java.time.format.*
import java.time.temporal.*

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
            if(!this.isSupported(ChronoField.OFFSET_SECONDS))
                null
            else {
                val offsetSeconds = this.get(ChronoField.OFFSET_SECONDS)
                if (offsetSeconds % 60 != 0) {
                    throw EvaluationException(
                        "The parsed timestamp has a UTC offset that not a multiple of 1 minute. " +
                        "This timestamp cannot be parsed accurately because the maximum " +
                        "resolution for an Ion timestamp offset is 1 minute.",
                        ErrorCode.EVALUATOR_PRECISION_LOSS_WHEN_PARSING_TIMESTAMP,
                        internal = false)
                }
                offsetSeconds / 60
            }

        /**
         * Parses a string given the specified format pattern.
         */
        fun parseTimestamp(timestampString: String, formatPattern: String): Timestamp {
            val pattern = FormatPattern.fromString(formatPattern)
            //TODO: do this during compilation
            pattern.validateForTimestampParsing()

            val accessor: TemporalAccessor by lazy {
                try {
                    DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern(pattern.formatPatternString)
                        .toFormatter()
                        .parse(timestampString)

                    //DateTimeFormatter.ofPattern(formatPattern).parse(timestampString)
                }
                catch (ex: IllegalArgumentException) {
                    throw EvaluationException(ex, ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
                        internal = false)
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
                        //Note that this overload of Timestamp.forSecond(...) creates a timestamp with "fraction" precision.
                        Timestamp.forSecond(year,
                                            accessor.get(ChronoField.MONTH_OF_YEAR),
                                            accessor.get(ChronoField.DAY_OF_MONTH),
                                            accessor.get(ChronoField.HOUR_OF_DAY),
                                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                                            BigDecimal.valueOf(accessor.getLong(ChronoField.SECOND_OF_MINUTE)).add(
                                                secondsFraction) as BigDecimal,
                                            accessor.getLocalOffset())
                    }
                    TimestampField.SECOND_OF_MINUTE   -> {
                        //Note that this overload of Timestamp.forSecond(...) creates a timestamp with "second" precision.
                        Timestamp.forSecond(year,
                                            accessor.get(ChronoField.MONTH_OF_YEAR),
                                            accessor.get(ChronoField.DAY_OF_MONTH),
                                            accessor.get(ChronoField.HOUR_OF_DAY),
                                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                                            accessor.get(ChronoField.SECOND_OF_MINUTE),
                                            accessor.getLocalOffset())
                    }
                    TimestampField.MINUTE_OF_HOUR -> {
                        Timestamp.forMinute(year,
                                            accessor.get(ChronoField.MONTH_OF_YEAR),
                                            accessor.get(ChronoField.DAY_OF_MONTH),
                                            accessor.get(ChronoField.HOUR_OF_DAY),
                                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                                            accessor.getLocalOffset())
                    }
                    TimestampField.HOUR_OF_DAY -> {
                        Timestamp.forMinute(year,
                                            accessor.get(ChronoField.MONTH_OF_YEAR),
                                            accessor.get(ChronoField.DAY_OF_MONTH),
                                            accessor.get(ChronoField.HOUR_OF_DAY),
                                            0, //Ion Timestamp has no HOUR precision -- default minutes to 0
                                            accessor.getLocalOffset())
                    }
                    TimestampField.DAY_OF_MONTH  -> {
                        Timestamp.forDay(year,
                                         accessor.get(ChronoField.MONTH_OF_YEAR),
                                         accessor.get(ChronoField.DAY_OF_MONTH))
                    }
                    TimestampField.MONTH_OF_YEAR -> {
                        Timestamp.forMonth(year, accessor.get(ChronoField.MONTH_OF_YEAR))
                    }
                    TimestampField.YEAR          -> {
                        Timestamp.forYear(year)
                    }
                    TimestampField.AM_PM, TimestampField.OFFSET, null -> {
                        errNoContext("This code should be unreachable because AM_PM or OFFSET or null" +
                                     "should never the value of formatPattern.leastSignificantField by at this point",
                                     internal = true)
                    }
                }
            }
            //Can be thrown by Timestamp.for*(...) methods.
            catch(ex: IllegalArgumentException) {
                throw EvaluationException(ex,
                    ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
                    propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to formatPattern),
                    internal = false)
            }
            //Can be thrown by TemporalAccessor.get(ChronoField)
            catch(ex: DateTimeException) {
                throw EvaluationException(ex,
                     ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
                     propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to formatPattern),
                     internal = false)
            }
        }
    }
}