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

package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_DAY
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_MINUTE
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * This implementation handles edge cases that can not be supported by [OffsetTimestampLowPrecision], that is:
 * 1. The desired precision exceeds nanosecond.
 * 2. The desired timestamp exceeds the range of +18:00 to -18:00
 */
internal class OffsetTimestampHighPrecision private constructor(
    val date: Date,
    val time: TimeWithTimeZone,
    _inputIonTimestamp: com.amazon.ion.Timestamp? = null,
    _epochSecond: BigDecimal? = null,
) : TimestampWithTimeZone() {
    companion object {
        /**
         * Construct a Timestamp by concatenate a Date and a Time component.
         *
         * The time component should have a TimeZone.
         *
         * This should be called by other constructors for validation date / time.
         */
        @JvmStatic
        fun forDateTime(date: Date, time: TimeWithTimeZone): OffsetTimestampHighPrecision =
            OffsetTimestampHighPrecision(date, time, null, null)

        /**
         * Construct a timestamp value using date time field and a given precision.
         *
         * @param year Year field
         * @param month Month field
         * @param day Day field
         * @param hour Hour field
         * @param second Second field, include fraction decimalSecond
         * @param timeZone TimeZone field, see [TimeZone], null value indicates a timestamp without timezone value.
         */
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: BigDecimal,
            timeZone: TimeZone
        ): OffsetTimestampHighPrecision {
            val date = SqlDate.of(year, month, day)
            val time = OffsetTimeHighPrecision.of(hour, minute, second, timeZone)
            return forDateTime(date, time)
        }

        /**
         * Construct a PartiQL timestamp based on an Ion Timestamp.
         * The created timestamp always has [TimeZone] and arbitrary precision.
         * Notice that Ion Value allows for "lower precision", year, month, etc.
         * For example, `2023T` is a valid ion timestamp with year precision.
         * This method always returns a "full timestamp expression", i.e., 2023-01-01T00:00:00.
         * At the moment there is no intention on preserving this.
         */
        @JvmStatic
        fun forIonTimestamp(ionTs: com.amazon.ion.Timestamp): OffsetTimestampHighPrecision {
            val timestamp = when (ionTs.localOffset) {
                null ->
                    of(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UnknownTimeZone
                    ).copy(_inputIonTimestamp = ionTs)

                else ->
                    of(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UtcOffset.of(ionTs.localOffset)
                    ).copy(_inputIonTimestamp = ionTs)
            }
            return timestamp
        }

        /**
         * Returns a timestamp based on epoch decimalSecond.
         *
         * The resulting timestamp is always a timestamp with timezone,
         * this is because epoch decimalSecond by definition refers to a point in time.
         * and timestamp without time zone does not refer to a point forEpochSecond time.
         */
        @JvmStatic
        fun forEpochSeconds(
            epochSeconds: BigDecimal,
            timeZone: TimeZone = TimeZone.UnknownTimeZone,
        ): OffsetTimestampHighPrecision {
            // hack to bootstrap attribute calculation
            val modifiedEpoch = when (timeZone) {
                TimeZone.UnknownTimeZone -> epochSeconds
                is TimeZone.UtcOffset ->
                    timeZone.totalOffsetMinutes.toBigDecimal()
                        .multiply(BigDecimal.valueOf(60))
                        .plus(epochSeconds)
            }
            val offsetDateTime =
                java.time.Instant.ofEpochSecond(modifiedEpoch.setScale(0, RoundingMode.DOWN).longValueExact()).atOffset(
                    ZoneOffset.UTC
                )

            val year = offsetDateTime.year
            val month = offsetDateTime.monthValue
            val day = offsetDateTime.dayOfMonth
            val hour = offsetDateTime.hour
            val minute = offsetDateTime.minute
            val wholeSecond = offsetDateTime.second
            val fractionSecond =
                epochSeconds.minus(BigDecimal.valueOf(epochSeconds.setScale(0, RoundingMode.DOWN).longValueExact()))
            return of(
                year, month, day,
                hour, minute,
                fractionSecond.add(BigDecimal.valueOf(wholeSecond.toLong())),
                timeZone
            )
        }
    }

    override val year: Int = date.year
    override val month: Int = date.month
    override val day: Int = date.day
    override val hour: Int = time.hour
    override val minute: Int = time.minute
    override val decimalSecond: BigDecimal = time.decimalSecond
    override val timeZone: TimeZone = time.timeZone

    @Deprecated("We will not store raw Ion Timestamp Value in the next release.")
    override val ionRaw: com.amazon.ion.Timestamp? = _inputIonTimestamp
    override val epochSecond: BigDecimal by lazy {
        _epochSecond ?: when (val timeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> getUTCEpoch(0)
            is TimeZone.UtcOffset -> getUTCEpoch(timeZone.totalOffsetMinutes)
        }
    }

    override fun plusYears(years: Long): OffsetTimestampHighPrecision =
        forDateTime(this.date.plusYears(years), this.time)

    override fun plusMonths(months: Long): OffsetTimestampHighPrecision =
        forDateTime(this.date.plusMonths(months), this.time)

    override fun plusDays(days: Long): OffsetTimestampHighPrecision =
        forDateTime(this.date.plusDays(days), this.time)

    override fun plusHours(hours: Long): OffsetTimestampHighPrecision =
        forEpochSeconds(this.epochSecond.plus((hours * SECONDS_IN_HOUR).toBigDecimal()), timeZone)

    override fun plusMinutes(minutes: Long): OffsetTimestampHighPrecision =
        forEpochSeconds(this.epochSecond.plus((minutes * SECONDS_IN_MINUTE).toBigDecimal()), timeZone)

    override fun plusSeconds(seconds: BigDecimal): OffsetTimestampHighPrecision =
        forEpochSeconds(this.epochSecond.plus(seconds.toBigDecimal()), timeZone)

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): LocalTimestampHighPrecision =
        this.atTimeZone(timeZone).let {
            LocalTimestampHighPrecision.of(it.year, it.month, it.day, it.hour, it.minute, it.decimalSecond)
        }

    override fun atTimeZone(timeZone: TimeZone): OffsetTimestampHighPrecision =
        when (val valueTimeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> {
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> this
                    is TimeZone.UtcOffset -> of(
                        year, month, day, hour, minute, decimalSecond, TimeZone.UtcOffset.of(0)
                    ).atTimeZone(timeZone)
                }
            }

            is TimeZone.UtcOffset -> {
                val utc = this.plusMinutes(-valueTimeZone.totalOffsetMinutes.toLong())
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> of(
                        utc.year, utc.month, utc.day,
                        utc.hour, utc.minute, utc.decimalSecond,
                        timeZone
                    )
                    is TimeZone.UtcOffset -> {
                        if (valueTimeZone == timeZone) this
                        else utc.plusMinutes(timeZone.totalOffsetMinutes.toLong())
                            .let { of(it.year, it.month, it.day, it.hour, it.minute, it.decimalSecond, timeZone) }
                    }
                }
            }
        }

    private fun getUTCEpoch(totalOffsetMinutes: Int): BigDecimal {
        val epochDay = LocalDate.of(year, month, day).toEpochDay()
        // Deal with time zone first, so we delay big decimal op
        val adjustForTimeZoneInSecond = epochDay * SECONDS_IN_DAY - totalOffsetMinutes * SECONDS_IN_MINUTE

        return BigDecimal.valueOf(adjustForTimeZoneInSecond).plus(this.time.elapsedSecond)
    }
    internal fun copy(
        _inputIonTimestamp: com.amazon.ion.Timestamp? = null,
        _epochSecond: BigDecimal? = null
    ) =
        OffsetTimestampHighPrecision(
            this.date, this.time,
            _inputIonTimestamp ?: this.ionTimestampValue, _epochSecond ?: this.epochSecond
        )
}
