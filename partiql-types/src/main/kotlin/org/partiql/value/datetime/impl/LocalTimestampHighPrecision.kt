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

import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * This implementation handles edge cases that can not be supported by [LocalTimestampLowPrecision], that is:
 * 1. The desired precision exceeds nanosecond.
 * 2. The desired timestamp exceeds the range of +18:00 to -18:00
 *
 */
internal class LocalTimestampHighPrecision private constructor(
    val date: SqlDate,
    val time: LocalTimeHighPrecision
) : TimestampWithoutTimeZone() {
    companion object {
        /**
         * Construct a timestamp without time zone value using its date component and time component.
         *
         * Notes the time component can not have time zone.
         */
        @JvmStatic
        fun forDateTime(date: SqlDate, time: LocalTimeHighPrecision): LocalTimestampHighPrecision =
            LocalTimestampHighPrecision(date, time)

        /**
         * Construct a timestamp value using date time field.
         *
         * @param year Year field
         * @param month Month field
         * @param day Day field
         * @param hour Hour field
         * @param decimalSecond Second field, include fraction decimalSecond
         */
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal
        ): LocalTimestampHighPrecision {
            val date = SqlDate.of(year, month, day)
            val time = LocalTimeHighPrecision.of(hour, minute, decimalSecond)
            return forDateTime(date, time)
        }
    }

    override val year: Int = date.year
    override val month: Int = date.month
    override val day: Int = date.day
    override val hour: Int = time.hour
    override val minute: Int = time.minute
    override val decimalSecond: BigDecimal = time.decimalSecond

    override fun plusYears(years: Long): LocalTimestampHighPrecision =
        forDateTime(this.date.plusYears(years), this.time)

    override fun plusMonths(months: Long): LocalTimestampHighPrecision =
        forDateTime(this.date.plusMonths(months), this.time)

    override fun plusDays(days: Long): LocalTimestampHighPrecision =
        forDateTime(this.date.plusDays(days), this.time)

    override fun plusHours(hours: Long): LocalTimestampHighPrecision {
        val rawHour = this.hour + hours
        val daysToCarry = if (rawHour >= 0) rawHour / 24 else rawHour / 24 - 1
        val afterHour = (rawHour % 24).toInt().let { if (it < 0) it + 24 else it }
        return forDateTime(date.plusDays(daysToCarry), LocalTimeHighPrecision.of(afterHour, minute, decimalSecond))
    }

    override fun plusMinutes(minutes: Long): LocalTimestampHighPrecision {
        val rawMinute = this.minute + minutes
        val hoursToCarry = if (rawMinute >= 0) rawMinute / 60 else rawMinute / 60 - 1
        val afterMinute = (rawMinute % 60).toInt().let { if (it < 0) it + 60 else it }
        return this.plusHours(hoursToCarry)
            .let { of(it.year, it.month, it.day, it.hour, afterMinute, it.decimalSecond) }
    }

    override fun plusSeconds(seconds: BigDecimal): LocalTimestampHighPrecision {
        val rawSecond = decimalSecond.plus(seconds)
        val minutesToCarry = if (rawSecond >= BigDecimal.ZERO) {
            rawSecond.div(BigDecimal.valueOf(60L))
        } else {
            rawSecond.div(BigDecimal.valueOf(60L)).minus(BigDecimal.ONE)
        }.setScale(0, RoundingMode.DOWN).longValueExact()
        val afterSecond =
            rawSecond
                .rem(BigDecimal.valueOf(60L))
                .let {
                    if (it < BigDecimal.ZERO) it.add(BigDecimal.valueOf(60L))
                    else it
                }
        return this.plusMonths(minutesToCarry).let { of(it.year, it.month, it.day, it.hour, it.minute, afterSecond) }
    }

    override fun withTimeZone(timeZone: TimeZone): OffsetTimestampHighPrecision =
        OffsetTimestampHighPrecision.of(
            year, month, day, hour, minute, decimalSecond, timeZone
        )
}
