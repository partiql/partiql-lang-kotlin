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

package org.partiql.lang.eval.builtins.timestamp

import org.partiql.lang.eval.builtins.timestamp.TimestampField.*

/**
 * A item that is parsed from the format pattern. i.e. text or one of many symbols corresponding to a
 * field and its formatting options.
 */
internal sealed class FormatItem
/**
 * Literal text to be included in the timestamp format.  Typically, `-`, `/` `:` ` ` or literal strings enclosed in `'`.
 */
internal data class TextItem(val raw: String) : FormatItem()

/**
 * Indicates the various fields of a timestamp.
 * The [precisionRank] field specifies the order of the precision such that higher [precisionRank] values are more
 * precise fields.  The [precisionRank] field is null for fields such as [AM_PM] and [OFFSET] which do not imply
 * a precision.
 */
internal enum class TimestampField(val precisionRank: Int? = null) {
    YEAR(0), MONTH_OF_YEAR(1), DAY_OF_MONTH(2), HOUR_OF_DAY(3), AM_PM(), MINUTE_OF_HOUR(4), SECOND_OF_MINUTE(5), FRACTION_OF_SECOND(6), OFFSET()
}

/**
 * Base class for all format symbols
 */
internal sealed class PatternSymbol : FormatItem() {
    abstract val field: TimestampField
}

/**
 * Species specific format for the year field.
 */
internal enum class YearFormat {
    /**
     * Format symbol: `yy`.
     */
    TWO_DIGIT,
    /**
     * Format symbol: `y`.
     */
    FOUR_DIGIT,
    /**
     * Format symbol: `yyyy`.
     */
    FOUR_DIGIT_ZERO_PADDED
}

/**
 * One of the format symbols corresponding to the year timestamp field, i.e. y, yy, yyyy.
 */
internal data class YearPatternSymbol(val format: YearFormat) : PatternSymbol() {
    override val field: TimestampField = TimestampField.YEAR
}

/**
 * Specifies specific format for the month field.
 */
internal enum class MonthFormat {
    /**
     * Format symbol: `M`.
     */
    MONTH_NUMBER,

    /**
     * Format symbol: `MM`.
     */
    MONTH_NUMBER_ZERO_PADDED,

    /**
     * Format symbol: `MMM`.
     */
    ABBREVIATED_MONTH_NAME,

    /**
     * Format symbol: `MMMM`.
     */
    FULL_MONTH_NAME,

    /**
     * Format symbol: `MMMMM`.
     */
     FIRST_LETTER_OF_MONTH_NAME,
}

/**
 * One of the format symbols corresponding to the month timestamp field, i.e. `M, `MM`, `MMM` or `MMMM`.
 */
internal data class MonthPatternSymbol(val format: MonthFormat) : PatternSymbol() {
    override val field: TimestampField = TimestampField.MONTH_OF_YEAR
}

/**
 * Generic formatting options shared by [DayOfMonthPatternSymbol], [MinuteOfHourPatternSymbol]
 * and [SecondOfMinutePatternPatternSymbol].
 */
internal enum class TimestampFieldFormat {

    /**
     * Format symbol: `y`.
     */
    NUMBER,
    ZERO_PADDED_NUMBER
}

/**
 * One of the format symbols corresponding to the day-of-month timestamp field, i.e. `d` or `dd`.
 */
internal data class DayOfMonthPatternSymbol(val format: TimestampFieldFormat) : PatternSymbol() {
    override val field: TimestampField = TimestampField.DAY_OF_MONTH
}

/**
 * Indicates if the hour-of-day field is in 12 or 24-hour format.
 */
internal enum class HourClock {
    TwelveHour,
    TwentyFourHour
}

/**
 * Specifies the specific format of the hour-of-day timestamp field.
 */
internal enum class HourOfDayFormatFieldFormat(val clock: HourClock) {
    /**
     * Format symbol: `h`.
     */
    NUMBER_12_HOUR(HourClock.TwelveHour),
    /**
     * Format symbol: `hh`.
     */
    ZERO_PADDED_NUMBER_12_HOUR(HourClock.TwelveHour),

    /**
     * Format symbol: `H`.
     */
    NUMBER_24_HOUR(HourClock.TwentyFourHour),

    /**
     * Format symbol: `HH`.
     */
    ZERO_PADDED_NUMBER_24_HOUR(HourClock.TwentyFourHour)
}

/**
 * One one of the format symbols corresponding to the hour-of-day timestamp field, i.e. `h`, `hh`, `H` or `HH`.
 */
internal data class HourOfDayPatternSymbol(val format: HourOfDayFormatFieldFormat) : PatternSymbol() {
    override val field: TimestampField = TimestampField.HOUR_OF_DAY
}

/**
 * One of the format symbols corresponding to the minute-of-hour timestamp field, i.e. `m` or `mm`.
 */
internal data class MinuteOfHourPatternSymbol(val format: TimestampFieldFormat) : PatternSymbol() {
    override val field: TimestampField = TimestampField.MINUTE_OF_HOUR
}

/**
 * One of the format symbols corresponding to the second-of-minute timestamp field, i.e. `s` or `ss`.
 */
internal data class SecondOfMinutePatternPatternSymbol(val format: TimestampFieldFormat) : PatternSymbol() {
    override val field: TimestampField = TimestampField.SECOND_OF_MINUTE
}

/**
 * Represents the nano-of-second timestamp field:  `n`.
 */
internal class NanoOfSecondPatternSymbol : PatternSymbol() {
    override val field: TimestampField = TimestampField.FRACTION_OF_SECOND

    /**
     * This is normally provided by kotlin for data classes but this can't be a data class because it doesn't require
     * any constructor arguments.
     */
    override fun equals(other: Any?) = this.javaClass.isInstance(other)

    /**
     * This is normally provided by kotlin for data classes but this can't be a data class because it doesn't require
     * any constructor arguments.
     */
    override fun hashCode(): Int = field.hashCode()
}


/**
 * Represents the AM-PM "pseudo" timestamp field:  `a`.
 */
internal class AmPmPatternSymbol : PatternSymbol() {
    override val field: TimestampField = TimestampField.AM_PM

    /**
     * This is normally provided by kotlin for data classes but this can't be a data class because it doesn't require
     * any constructor arguments.
     */
    override fun equals(other: Any?) = this.javaClass.isInstance(other)

    /**
     * This is normally provided by kotlin for data classes but this can't be a data class because it doesn't require
     * any constructor arguments.
     */
    override fun hashCode(): Int = field.hashCode()
}

/**
 * Represents the fraction-of-second timestamp field:  `S`, which has variable precision indicated by the number of
 * consecutive `S` symbols and is specified by [precision], i.e. `S` has a precision of 1 whlie `SSSSSS` has a
 * precision of 6.
 */
internal data class FractionOfSecondPatternSymbol(val precision: Int) : PatternSymbol() {
    override val field: TimestampField = TimestampField.FRACTION_OF_SECOND
}

internal enum class OffsetFieldFormat {
    /**
     * Format symbol: `x`.
     */
    ZERO_PADDED_HOUR,
    /**
     * Format symbol: `xx` or `xxxx`.
     */

    ZERO_PADDED_HOUR_MINUTE,
    /**
     * Format symbol: `xxx` or `xxxxx`.
     */
    ZERO_PADDED_HOUR_COLON_MINUTE,

    /**
     * Format symbol: `X`.
     */
    ZERO_PADDED_HOUR_OR_Z,

    /**
     * Format symbol: `XX` or `XXXX`.
     */
    ZERO_PADDED_HOUR_MINUTE_OR_Z,

    /**
     * Format symbol: `XXX` or `XXXXX`
     */
    ZERO_PADDED_HOUR_COLON_MINUTE_OR_Z,
}

/**
 * One of the format symbols corresponding to the offset timestamp field, i.e.: `x`, `xx`, `xxx`, `xxxx`, `X`, `XX`,
 * `XXX`, or `XXXX`.
 */
internal data class OffsetPatternSymbol(val format: OffsetFieldFormat) : PatternSymbol() {
    override val field: TimestampField = TimestampField.OFFSET
}