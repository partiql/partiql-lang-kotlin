/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.datetime

import java.math.BigDecimal

/**
 * Superclass for all classes representing datetime values.
 */
public sealed interface Datetime {
    /**
     * Year field of date time object
     */
    public val year: Int?

    /**
     * Month field of date time object
     */
    public val month: Int?

    /**
     * Day field of date time object
     */
    public val day: Int?

    /**
     * Hour field of date time object
     */
    public val hour: Int?

    /**
     * Minute field of date time object
     */
    public val minute: Int?

    /**
     * Second field of date time object.
     * This field includes second fraction.
     */
    public val decimalSecond: BigDecimal?

    /**
     * Time zone field of date time object. See [Timezone]
     */
    public val timeZone: Timezone?

    /**
     * Equals method.
     * Two [Datetime] values are considered equals if and only if all the fields are equals.
     */
    public override fun equals(other: Any?): Boolean

    public override fun hashCode(): Int

    public override fun toString(): String

    public companion object {

        /**
         * Create a timestamp value.
         *
         * If time zone is null, then the value created is timestamp without timezone.
         * Otherwise, a timestamp with timezone is created.
         *
         * @param year Proleptic Year
         * @param month Month of Year
         * @param day Day Of Month
         * @param hour Hour of Day
         * @param minute Minute of Hour
         * @param second Second, include any fraction second.
         * @param timeZone TimeZone offset, see [Timezone]
         */
        @JvmStatic
        @JvmOverloads
        public fun timestamp(
            year: Int,
            month: Int = 1,
            day: Int = 1,
            hour: Int = 0,
            minute: Int = 0,
            second: BigDecimal = BigDecimal.ZERO,
            timeZone: Timezone? = null,
        ): Timestamp = when (timeZone) {
            Timezone.UnknownTimeZone -> {
                if (second.scale() <= 9) {
                    OffsetTimestampLowPrecision.of(year, month, day, hour, minute, second, timeZone)
                } else {
                    OffsetTimestampHighPrecision.of(year, month, day, hour, minute, second, timeZone)
                }
            }
            is Timezone.UtcOffset -> {
                if (timeZone.totalOffsetMinutes.absoluteValue > JAVA_MAX_OFFSET) {
                    OffsetTimestampHighPrecision.of(year, month, day, hour, minute, second, timeZone)
                } else if (second.scale() <= 9) {
                    OffsetTimestampLowPrecision.of(year, month, day, hour, minute, second, timeZone)
                } else {
                    OffsetTimestampHighPrecision.of(year, month, day, hour, minute, second, timeZone)
                }
            }

            null -> {
                if (second.scale() <= 9) LocalTimestampLowPrecision.of(year, month, day, hour, minute, second)
                else LocalTimestampHighPrecision.of(year, month, day, hour, minute, second)
            }
        }

        /**
         * Create a timestamp value.
         * The timestamp created will have precision 0 (no fractional second).
         *
         * @param year Proleptic Year
         * @param month Month of Year
         * @param day Day Of Month
         * @param hour Hour of Day
         * @param minute Minute of Hour
         * @param second whole Second.
         * @param timeZone TimeZone offset, see [Timezone]
         */
        @JvmStatic
        @JvmOverloads
        public fun timestamp(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: Int,
            timeZone: Timezone? = null,
        ): Timestamp = timestamp(year, month, day, hour, minute, second.toBigDecimal(), timeZone)

        /**
         * Create a timestamp value.
         * If time is an instance of [TimeWithTimeZone], then the timestamp created will be [TimestampWithTimeZone],
         * Otherwise it will be a [TimestampWithoutTimeZone].
         *
         * @param date: Date. See [Date]
         * @param time: Time. See [Time]
         */
        @JvmStatic
        public fun timestamp(date: Date, time: Time): Timestamp = when (time) {
            is TimeWithTimeZone -> timestamp(
                date.year, date.month, date.day, time.hour, time.minute, time.decimalSecond, time.timeZone
            )

            is TimeWithoutTimeZone -> timestamp(
                date.year, date.month, date.day, time.hour, time.minute, time.decimalSecond
            )
        }

        /**
         * Create a timestamp value based on [com.amazon.ion.Timestamp]
         * The created timestamp will always be an instance of [TimestampWithTimeZone]
         */
        @JvmStatic
        public fun timestamp(ionTimestamp: com.amazon.ion.Timestamp): TimestampWithTimeZone =
            if (ionTimestamp.localOffset != null && ionTimestamp.localOffset.absoluteValue > JAVA_MAX_OFFSET) {
                OffsetTimestampHighPrecision.forIonTimestamp(ionTimestamp)
            } else if (ionTimestamp.decimalSecond.scale() <= 9) {
                OffsetTimestampLowPrecision.forIonTimestamp(ionTimestamp)
            } else {
                OffsetTimestampHighPrecision.forIonTimestamp(ionTimestamp)
            }

        /**
         * Create a timestamp value based on displacement of Unix Epoch, at given time zone.
         * The created timestamp will always be an instance of [TimestampWithTimeZone]
         */
        @JvmStatic
        public fun timestamp(epochSeconds: BigDecimal, timeZone: Timezone): TimestampWithTimeZone = when (timeZone) {
            Timezone.UnknownTimeZone -> {
                if (epochSeconds.scale() <= 9) {
                    OffsetTimestampLowPrecision.forEpochSeconds(epochSeconds, timeZone)
                } else {
                    OffsetTimestampHighPrecision.forEpochSeconds(epochSeconds, timeZone)
                }
            }
            is Timezone.UtcOffset -> {
                if (timeZone.totalOffsetMinutes > JAVA_MAX_OFFSET) {
                    OffsetTimestampHighPrecision.forEpochSeconds(epochSeconds, timeZone)
                } else if (epochSeconds.scale() <= 9) {
                    OffsetTimestampLowPrecision.forEpochSeconds(epochSeconds, timeZone)
                } else {
                    OffsetTimestampHighPrecision.forEpochSeconds(epochSeconds, timeZone)
                }
            }
        }

        /**
         * Create a time value.
         *
         * If time zone is null, then the value created is time without timezone.
         * Otherwise, a time with timezone is created.
         *
         * @param hour Hour of Day
         * @param minute Minute of Hour
         * @param second Second, include any fraction second.
         * @param timeZone TimeZone offset, see [Timezone]
         */
        @JvmStatic
        @JvmOverloads
        public fun time(
            hour: Int,
            minute: Int,
            second: BigDecimal,
            timeZone: Timezone? = null,
        ): Time = when (timeZone) {
            Timezone.UnknownTimeZone -> {
                if (second.scale() <= 9) {
                    OffsetTimeLowPrecision.of(hour, minute, second, timeZone)
                } else {
                    OffsetTimeHighPrecision.of(hour, minute, second, timeZone)
                }
            }
            is Timezone.UtcOffset -> {
                if (timeZone.totalOffsetMinutes.absoluteValue > JAVA_MAX_OFFSET) {
                    OffsetTimeHighPrecision.of(hour, minute, second, timeZone)
                } else if (second.scale() <= 9) {
                    OffsetTimeLowPrecision.of(hour, minute, second, timeZone)
                } else {
                    OffsetTimeHighPrecision.of(hour, minute, second, timeZone)
                }
            }

            null -> {
                if (second.scale() <= 9) LocalTimeLowPrecision.of(hour, minute, second)
                else LocalTimeHighPrecision.of(hour, minute, second)
            }
        }

        /**
         * Create a time value.
         * The time created will have precision 0 (no fractional second).
         *
         * @param hour Hour of Day
         * @param minute Minute of Hour
         * @param second whole Second.
         * @param timeZone TimeZone offset, see [Timezone]
         */
        @JvmStatic
        @JvmOverloads
        public fun time(
            hour: Int,
            minute: Int,
            second: Int,
            timeZone: Timezone? = null,
        ): Time = time(hour, minute, second.toBigDecimal(), timeZone)

        /**
         * Create a time value.
         * The time created will have precision 9 (nanosecond precision).
         *
         * @param hour Hour of Day
         * @param minute Minute of Hour
         * @param second whole Second.
         * @param nano Nano offset.
         * @param timeZone TimeZone offset, see [Timezone]
         */
        @JvmStatic
        @JvmOverloads
        public fun time(
            hour: Int,
            minute: Int,
            second: Int,
            nano: Int,
            timeZone: Timezone? = null,
        ): Time {
            val decimalSecond = second.toBigDecimal().plus(nano.toBigDecimal().movePointLeft(9))
            return time(hour, minute, decimalSecond, timeZone)
        }

        @JvmStatic
        public fun date(
            year: Int,
            month: Int,
            day: Int,
        ): Date = SqlDate.of(year, month, day)
    }
}

