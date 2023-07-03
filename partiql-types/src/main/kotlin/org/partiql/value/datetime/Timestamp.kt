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
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.jvm.Throws
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
        public fun of(date: Date, time: Time): Timestamp =
            Timestamp(
                date.year, date.month, date.day,
                time.hour, time.minute, time.second,
                time.timeZone, time.precision
            )

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
        @JvmStatic
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
                true -> of(date.plusDays(1L), roundedTime)
                false -> {
                    of(date, roundedTime)
                }
            }
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
        public fun of(ionTs: TimestampIon): Timestamp {
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
    @get:Throws(DateTimeException::class)
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

    private fun getUTCEpoch(totalOffsetMinutes: Int): BigDecimal {
        val epochDay = LocalDate.of(year, month, day).toEpochDay()
        val excludedSecond = epochDay * SECONDS_IN_DAY + hour * SECONDS_IN_HOUR + minute * SECONDS_IN_MINUTE
        // since offset does not include second field, we can adjust it here, and leave bigDecimal calculation later
        val adjusted = excludedSecond - totalOffsetMinutes * SECONDS_IN_MINUTE
        return second.plus(BigDecimal.valueOf(adjusted))
    }
}
