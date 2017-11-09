package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
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

    internal enum class FormatPatternPrecision {
        UNKNOWN, YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, FRACTION;
    }

    internal data class FormatPatternInfo(val precision: FormatPatternPrecision, val has2DigitYear: Boolean) {
        companion object {
            private val nonSymbolAllowedChars = listOf(' ', '/', '-', ',', ':', '.')

            /**
             * Examines a format pattern and makes an educated guess as to the timestamp precision of the format.
             * This is probably less than ideal, however it doesn't seem that [DateTimeFormatter] exposes this
             * information in a way that will work for all precisions.
             *
             * @return The identified FormatPatternPrecsion.  If FormatPatternPrecision.UNKNOWN is returned, the format
             * pattern uses an unrecognized character that is not enclosed in single quotes.
             */
            fun fromPattern(formatPattern: String): FormatPatternInfo {
                var inQuote = false
                var hasFraction = false
                var hasSecond = false
                var hasMinute = false
                var hasHour = false
                var hasDay = false
                var hasMonth = false
                var yearCount = 0

                for(c in formatPattern) {
                    when {
                        c == '\''                             -> inQuote = !inQuote
                        inQuote || c in nonSymbolAllowedChars -> { /* continue to next */ }
                        c == 'S' || c == 'n'                  -> hasFraction = true
                        c == 's'                              -> hasSecond = true
                        c == 'm'                              -> hasMinute = true
                        c == 'H' || c == 'h'                  -> hasHour = true
                        c == 'd'                              -> hasDay = true
                        c == 'M'                              -> hasMonth = true
                        c == 'y'                              -> yearCount++
                        !inQuote && !TIMESTAMP_FORMAT_SYMBOLS.contains(c) ->
                            return FormatPatternInfo(FormatPatternPrecision.UNKNOWN, false)
                    }
                }

                return FormatPatternInfo(
                    when {
                        hasFraction ->
                            FormatPatternPrecision.FRACTION
                        hasSecond ->
                            FormatPatternPrecision.SECOND
                        hasMinute ->
                            FormatPatternPrecision.MINUTE
                        hasHour ->
                            FormatPatternPrecision.HOUR
                        hasDay ->
                            FormatPatternPrecision.DAY
                        hasMonth ->
                            FormatPatternPrecision.MONTH
                        yearCount > 0 ->
                            FormatPatternPrecision.YEAR
                        else ->
                            FormatPatternPrecision.UNKNOWN
                    },
                    yearCount == 2)
            }
        }
    }

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
            val info = FormatPatternInfo.fromPattern(formatPattern)
            val accessor: TemporalAccessor by lazy {
                try {
                    DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(formatPattern).toFormatter().parse(timestampString)
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
                    !info.has2DigitYear || year < TWO_DIGIT_PIVOT_YEAR + 2000 -> year
                    else -> year - 100
                }
            }

            return try {
                when (info.precision) {
                   FormatPatternPrecision.UNKNOWN -> {
                        throw EvaluationException(
                            "The specified format pattern contains invalid format symbols or is otherwise invalid",
                            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN,
                            PropertyValueMap().also {
                                it[Property.TIMESTAMP_FORMAT_PATTERN] = formatPattern
                            }, internal = false)
                    }
                    FormatPatternPrecision.FRACTION -> {
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
                    FormatPatternPrecision.SECOND -> {
                        //Note that this overload of Timestamp.forSecond(...) creates a timestamp with "second" precision.
                        Timestamp.forSecond(year,
                                            accessor.get(ChronoField.MONTH_OF_YEAR),
                                            accessor.get(ChronoField.DAY_OF_MONTH),
                                            accessor.get(ChronoField.HOUR_OF_DAY),
                                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                                            accessor.get(ChronoField.SECOND_OF_MINUTE),
                                            accessor.getLocalOffset())
                    }
                    FormatPatternPrecision.MINUTE -> {
                        Timestamp.forMinute(year,
                                            accessor.get(ChronoField.MONTH_OF_YEAR),
                                            accessor.get(ChronoField.DAY_OF_MONTH),
                                            accessor.get(ChronoField.HOUR_OF_DAY),
                                            accessor.get(ChronoField.MINUTE_OF_HOUR),
                                            accessor.getLocalOffset())
                    }
                     FormatPatternPrecision.HOUR -> {
                        Timestamp.forMinute(year,
                                            accessor.get(ChronoField.MONTH_OF_YEAR),
                                            accessor.get(ChronoField.DAY_OF_MONTH),
                                            accessor.get(ChronoField.HOUR_OF_DAY),
                                            0, //Ion Timestamp has no HOUR precision -- default minutes to 0
                                            accessor.getLocalOffset())
                    }
                    FormatPatternPrecision.DAY -> {
                        Timestamp.forDay(year,
                                         accessor.get(ChronoField.MONTH_OF_YEAR),
                                         accessor.get(ChronoField.DAY_OF_MONTH))
                    }
                    FormatPatternPrecision.MONTH -> {
                        Timestamp.forMonth(year, accessor.get(ChronoField.MONTH_OF_YEAR))
                    }
                    FormatPatternPrecision.YEAR -> {
                        Timestamp.forYear(year)
                    }
                }
            }
            //Can be thrown by Timestamp.for*(...) methods.
            catch(ex: IllegalArgumentException) {
                throw EvaluationException(ex,
                    ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
                    PropertyValueMap().also { it[Property.TIMESTAMP_FORMAT_PATTERN] = formatPattern },
                    internal = false)
            }
            //Can be thrown by TemporalAccessor.get(ChronoField)
            catch(ex: DateTimeException) {
                throw EvaluationException(ex,
                     ErrorCode.EVALUATOR_CUSTOM_TIMESTAMP_PARSE_FAILURE,
                     PropertyValueMap().also { it[Property.TIMESTAMP_FORMAT_PATTERN] = formatPattern },
                    internal = false)
            }
        }
    }
}