/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_DAY
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_MINUTE
import org.partiql.value.datetime.Timestamp.Companion.equals
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.jvm.Throws
import kotlin.math.absoluteValue
import com.amazon.ion.Timestamp as TimestampIon

// TODO: Further break this down to tow implementation, one with nanosecond and below precision
//  and the other with nano-second and above precision, including arbitrary precision
//  The big decimal implementation is too slow and arguably for ion-compatibly reason only.
/**
 * This class is used to model both Timestamp Without Time Zone type and Timestamp With Time Zone Type.
 *
 * Two timestamp values are equal if and only if all the fields (including precision) are the same.
 *
 * Use [compareTo] if the goal is to check equivalence (refer to the same point in time).
 *
 * @param year Year field
 * @param month Month field
 * @param day Day field
 * @param hour Hour field
 * @param second Second field, include fraction second
 * @param timeZone TimeZone field, see [TimeZone], null value indicates a timestamp without timezone value.
 * @param precision If value is null, the timestamp has arbitrary precision.
 *                  If the value is non-null, the timestamp has a fixed precision.
 *                  (precision means number of digits in fraction second.)
 */
public data class Timestamp(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: BigDecimal,
    val timeZone: TimeZone?,
    val precision: Int?
) : Comparable<Timestamp> {
    public companion object {
        /**
         * Construct a timestamp value using date time field and a given precision.
         *
         * @param year Year field
         * @param month Month field
         * @param day Day field
         * @param hour Hour field
         * @param second Second field, include fraction second
         * @param timeZone TimeZone field, see [TimeZone], null value indicates a timestamp without timezone value.
         * @param precision If value is null, the timestamp has arbitrary precision.
         *                  If the value is non-null, the timestamp has a fixed precision.
         *                  (precision means number of digits in fraction second.)
         */
        public fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: BigDecimal,
            timeZone: TimeZone?,
            precision: Int? = null
        ): Timestamp {
            val date = Date.of(year, month, day)
            val arbitraryTime = Time.of(hour, minute, second, timeZone)
            val roundedTime = Time.of(hour, minute, second, timeZone, precision)
            // if the rounding result and the original result differs in more than 1 second, then we need to carry to date
            return when ((arbitraryTime.elapsedSecond - roundedTime.elapsedSecond).abs() > BigDecimal.ONE) {
                true -> forDateTime(date.plusDays(1L), roundedTime)
                false -> {
                    forDateTime(date, roundedTime)
                }
            }
        }

        /**
         * The intention of this API is to create a Timestamp using a Date and a Time component.
         * It is assumed that the time component has already been rounded,
         * meaning this API will not be responsible, if the rounding requires carrying in to day field.
         *
         * For example: the result of
         * Timestamp.of(Date.of(2023, 06, 01), Time.of(23, 59, 59.9, TimeZone.of(0,0), 0)
         * will result in
         * 2023-06-01T00:00:00+00:00
         *
         * If the desired result is `2023-06-02T00:00:00+00:00`, use [of]
         */
        @JvmStatic
        public fun forDateTime(date: Date, time: Time): Timestamp =
            Timestamp(
                date.year, date.month, date.day,
                time.hour, time.minute, time.second,
                time.timeZone, time.precision
            )

        /**
         * Construct a PartiQL timestamp based on an Ion Timestamp.
         * The created timestamp always has [TimeZone] and arbitrary precision.
         * Notice that Ion Value allows for "lower precision", year, month, etc.
         * For example, `2023T` is a valid ion timestamp with year precision.
         * This method always returns a "full timestamp expression", i.e., 2023-01-01T00:00:00.
         * At the moment there is no intention on preserving this.
         */
        @JvmStatic
        public fun forIonTimestamp(ionTs: TimestampIon): Timestamp {
            val timestamp = when (ionTs.localOffset) {
                null ->
                    Timestamp(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UnknownTimeZone,
                        null
                    )

                else ->
                    Timestamp(
                        ionTs.year, ionTs.month, ionTs.day,
                        ionTs.hour, ionTs.minute, ionTs.decimalSecond,
                        TimeZone.UtcOffset.of(ionTs.localOffset),
                        null
                    )
            }
            return timestamp.let {
                it.ionRaw = ionTs
                it
            }
        }

        /**
         * Returns a timestamp based on epoch second.
         *
         * The resulting timestamp is always a timestamp with timezone,
         * this is because epoch second by definition refers to a point in time.
         * and timestamp without time zone does not refer to a point forEpochSecond time.
         */
        @JvmStatic
        public fun forEpochSecond(
            seconds: BigDecimal,
            timeZone: TimeZone = TimeZone.UnknownTimeZone,
            precision: Int? = null
        ): Timestamp {
            val offsetDateTime = java.time.Instant.ofEpochSecond(seconds.setScale(0, RoundingMode.DOWN).longValueExact()).atOffset(ZoneOffset.UTC)
            val year = offsetDateTime.year
            val month = offsetDateTime.monthValue
            val day = offsetDateTime.dayOfMonth
            val hour = offsetDateTime.hour
            val minute = offsetDateTime.minute
            val wholeSecond = offsetDateTime.second
            val fractionSecond = seconds.minus(BigDecimal.valueOf(seconds.setScale(0, RoundingMode.DOWN).longValueExact()))
            return of(
                year, month, day,
                hour, minute,
                fractionSecond.add(BigDecimal.valueOf(wholeSecond.toLong())),
                timeZone,
                precision
            )
        }

        @JvmStatic
        public fun nowZ(): Timestamp =
            forEpochSecond(BigDecimal.valueOf(System.currentTimeMillis(), 3), TimeZone.UtcOffset.of(0))
    }

    /**
     * Returns an Ion Equivalent Timestamp
     * [ionTimestampValue] takes care of the precision,
     * the ion representation will have exact precision of the backing partiQL value.
     */
    @get:Throws(DateTimeException::class)
    val ionTimestampValue: TimestampIon by lazy {
        when (val timeZone = this.timeZone) {
            null -> throw DateTimeException("Timestamp without Time Zone has no corresponding Ion Value")
            TimeZone.UnknownTimeZone -> TimestampIon.forSecond(
                year, month, day,
                hour, minute, second,
                TimestampIon.UNKNOWN_OFFSET
            )

            is TimeZone.UtcOffset -> TimestampIon.forSecond(
                year, month, day,
                hour, minute, second,
                timeZone.totalOffsetMinutes
            )
        }
    }

    /**
     * Returns a BigDecimal representing the Timestamp's point in time that is
     * the number of Seconds (*including* any fractional Seconds)
     * from the epoch.
     *
     * Since PartiQL support a wider range than Unix Time,
     * timestamp value that is before 1970-01-01T00:00:00Z will have a negative epoch value.
     *
     * If a timestamp does not contain Information regarding timezone,
     * there is no way to assign a point in time for such value, therefore the method will throw an error.
     *
     * If a timestamp contains unknown timezone, by semantics its UTC value is known, we return the UTC value in epoch.
     *
     * This method will return the same result for all Timestamp values representing
     * the same point in time, regardless of the local offset.
     */
    val epochSecond: BigDecimal by lazy {
        when (val timeZone = this.timeZone) {
            null -> throw DateTimeException("Timestamp without time zone has no Epoch Second attribute.")
            TimeZone.UnknownTimeZone -> getUTCEpoch(0)
            is TimeZone.UtcOffset -> getUTCEpoch(timeZone.totalOffsetMinutes)
        }
    }

    val epochMillis: BigDecimal by lazy {
        epochSecond.movePointRight(3)
    }

    val date: Date by lazy { Date.of(year, month, day) }

    val time: Time by lazy { Time.of(hour, minute, second, timeZone, precision) }

    /**
     * For backward compatibility issue, we track the original Ion Input
     */
    @Deprecated(
        "We will not store raw Ion Timestamp Value in the next release.",
        replaceWith = ReplaceWith("ionTimestampValue")
    )
    var ionRaw: TimestampIon? = null

    /**
     * Comparison method for timestamp value
     * If one value is timestamp with time zone and the other is timestamp without time zone:
     *      Error
     *      One may not directly compare a timestamp value with timezone to a timestamp value without time zone.
     * If both value are timestamp with timezone:
     *      The two value are considered equivalent if they refer to the same point in time.
     * If both value are timestamp without timezone:
     *      The two value are consider equivalent if all the fields (exclude precision) are equivalent.
     *      Another way to interpret this is, two timestamp without timezone are equivalent
     *      if and only if they are equivalent when converted to the same time zone
     *
     * It is worth to distinguish between the [equals] method and the [compareTo] method.
     *
     * For example:
     * The [equals] method will return false for the following timestamp with time zone values, but [compareTo] will return 0.
     * ```
     * // Timestamp with time zone
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, null) // arbitrary precision 1970-01-01T00:00:00.000Z
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 1), 0, 0, null) // arbitrary precision 1970-01-01T00:00:00.0Z
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 0, 0, 3) // precision 3, 1970-01-01T00:00:00.000Z
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 1), 0, 0, 1) // precision 1, 1970-01-01T00:00:00.0Z
     * Timestamp(1969, 12, 31, 23, 59, BigDecimal.valueOf(59.9, 1), 0, 0, 0) // precision 0, 1970-01-01T00:00:00Z
     * Timestamp(1970, 1, 1, 1, 0, BigDecimal.valueOf(0, 1), 1, 0, 1) // precision 1, 1970-01-01T01:00:00.0+01:00
     * ```
     * The [equals] method will return false for the following timestamp without time zone values, but [compareTo] will return 0.
     * ```
     * // Timestamp with time zone
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), null) // arbitrary precision 1970-01-01T00:00:00.000
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 1), null) // arbitrary precision 1970-01-01T00:00:00.0
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 3), 3) // precision 3, 1970-01-01T00:00:00.000
     * Timestamp(1970, 1, 1, 0, 0, BigDecimal.valueOf(0, 1), 1) // precision 1, 1970-01-01T00:00:00.0
     * Timestamp(1969, 12, 31, 23, 59, BigDecimal.valueOf(59.9, 1), 0) // precision 0, 1970-01-01T00:00:00
     * ```
     *
     */
    @Throws(DateTimeException::class)
    public override fun compareTo(other: Timestamp): Int = when {
        this.timeZone != null && other.timeZone != null -> this.epochSecond.compareTo(other.epochSecond)
        // for timestamp without time zones, assume UTC and compare
        this.timeZone == null && other.timeZone == null -> {
            this.copy(timeZone = TimeZone.UtcOffset.of(0))
                .compareTo(other.copy(timeZone = TimeZone.UtcOffset.of(0)))
        }
        else -> throw DateTimeException(
            "Can not compare between timestamp with time zone and timestamp without time zone"
        )
    }

    //
    // Operation
    //
    /**
     * Returns the same instant,
     * but the data time fields in the instant will be different as the time zone is different.
     */
    public fun atTimeZone(timeZone: TimeZone): Timestamp {
        return when (this.timeZone) {
            TimeZone.UnknownTimeZone -> this.copy(timeZone = timeZone)
            is TimeZone.UtcOffset -> {
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> this.atTimeZone(TimeZone.UtcOffset.of(0)).copy(timeZone = timeZone)
                    is TimeZone.UtcOffset -> {
                        var _minute = this.minute - this.timeZone.tzMinute + timeZone.tzMinute
                        var _hour = this.hour - this.timeZone.tzHour + this.timeZone.tzHour
                        var hourOffset = _minute / 60
                        _minute %= 60
                        if (_minute < 0) {
                            _minute += 60
                            hourOffset -= 1
                        }
                        _hour += hourOffset
                        var dayOffset = _hour / 24
                        _hour %= 24
                        if (_hour < 0) {
                            _hour += 24
                            dayOffset -= 1
                        }
                        forDateTime(
                            Date.of(this.year, this.month, this.day).plusDays(dayOffset.toLong()),
                            Time.of(_hour, _minute, this.second, timeZone, this.precision)
                        )
                    }
                }
            }

            null -> TODO()
        }
    }
    public fun plusYear(years: Long): Timestamp = forDateTime(this.date.plusYear(years), this.time)
    public fun plusMonth(months: Long): Timestamp = forDateTime(this.date.plusMonths(months), this.time)
    public fun plusDays(days: Long): Timestamp = forDateTime(this.date.plusDays(days), this.time)
    public fun plusHours(hours: Long): Timestamp =
        when (this.timeZone) {
            // Timestamp without time zone, we handle it by "assume the time zone is UTC
            null -> {
                val ts = this.copy(timeZone = TimeZone.UtcOffset.of(0))
                ts.plusHours(hours).copy(timeZone = null)
            }

            TimeZone.UnknownTimeZone, is TimeZone.UtcOffset ->
                forEpochSecond(this.epochSecond.plus(BigDecimal.valueOf(hours * SECONDS_IN_HOUR)), timeZone, precision)
        }
    public fun plusMinutes(minutes: Long): Timestamp =
        when (this.timeZone) {
            // Timestamp without time zone, we handle it by "assume the time zone is UTC
            null -> {
                val ts = this.copy(timeZone = TimeZone.UtcOffset.of(0))
                ts.plusMinutes(minutes).copy(timeZone = null)
            }

            TimeZone.UnknownTimeZone, is TimeZone.UtcOffset ->
                forEpochSecond(this.epochSecond.plus(BigDecimal.valueOf(minutes * SECONDS_IN_MINUTE)), timeZone, precision)
        }
    public fun plusSeconds(seconds: Long): Timestamp =
        when (this.timeZone) {
            // Timestamp without time zone, we handle it by "assume the time zone is UTC
            null -> {
                val ts = this.copy(timeZone = TimeZone.UtcOffset.of(0))
                ts.plusSeconds(seconds).copy(timeZone = null)
            }

            TimeZone.UnknownTimeZone, is TimeZone.UtcOffset ->
                forEpochSecond(this.epochSecond.plus(BigDecimal.valueOf(seconds)), timeZone, precision)
        }
    public fun plusSeconds(seconds: BigDecimal): Timestamp =
        when (this.timeZone) {
            // Timestamp without time zone, we handle it by "assume the time zone is UTC
            null -> {
                val ts = this.copy(timeZone = TimeZone.UtcOffset.of(0))
                ts.plusSeconds(seconds).copy(timeZone = null)
            }

            TimeZone.UnknownTimeZone, is TimeZone.UtcOffset ->
                forEpochSecond(this.epochSecond.plus(seconds), timeZone, precision)
        }

    // Others Utils
    public fun toStringSQL(): String {
        val year = this.year.toString().padStart(4, '0')
        val month = this.month.toString().padStart(2, '0')
        val day = this.day.toString().padStart(2, '0')
        val hour = this.hour.toString().padStart(2, '0')
        val minute = this.minute.toString().padStart(2, '0')
        val second = when (this.second.scale()) {
            0 -> this.second.toPlainString().padStart(2, '0')
            else -> {
                val (whole, fraction) = this.second.toPlainString().split('.')
                "${whole.padStart(2, '0')}.$fraction"
            }
        }
        val withoutTz = "$year-$month-$day $hour:$minute:$second"
        return when (val timeZone = this.timeZone) {
            null -> withoutTz
            TimeZone.UnknownTimeZone -> "$withoutTz-00:00"
            is TimeZone.UtcOffset -> {
                val tzHour = timeZone.tzHour.absoluteValue.toString().padStart(2, '0')
                val tzMinute = timeZone.tzMinute.absoluteValue.toString().padStart(2, '0')
                if (timeZone.tzHour >= 0) "$withoutTz+$tzHour:$tzMinute"
                else "$withoutTz-$tzHour:$tzMinute"
            }
        }
    }

    private fun getUTCEpoch(totalOffsetMinutes: Int): BigDecimal {
        val epochDay = LocalDate.of(year, month, day).toEpochDay()
        val excludedSecond = epochDay * SECONDS_IN_DAY + hour * SECONDS_IN_HOUR + minute * SECONDS_IN_MINUTE
        // since offset does not include second field, we can adjust it here, and leave bigDecimal calculation later
        val adjusted = excludedSecond - totalOffsetMinutes * SECONDS_IN_MINUTE
        return second.plus(BigDecimal.valueOf(adjusted))
    }
}
