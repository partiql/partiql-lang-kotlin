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
import org.partiql.value.datetime.DateTimeUtil
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_DAY
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeZone
import java.math.BigDecimal

/**
 * This implementation handles edge cases that can not be supported by [OffsetTimeLowPrecision], that is:
 * 1. The desired precision exceeds nanosecond.
 * 2. The desired timestamp exceeds the range of +18:00 to -18:00
 */
internal class OffsetTimeHighPrecision private constructor(
    val localTime: LocalTimeHighPrecision,
    override val timeZone: TimeZone
) : TimeWithTimeZone() {

    companion object {
        fun of(
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal,
            timeZone: TimeZone
        ): OffsetTimeHighPrecision {
            val localTime = LocalTimeHighPrecision.of(hour, minute, decimalSecond)
            return OffsetTimeHighPrecision(localTime, timeZone)
        }

        fun forSeconds(elapsedSeconds: BigDecimal, timeZone: TimeZone): OffsetTimeHighPrecision {
            val localTime = LocalTimeHighPrecision.forSeconds(elapsedSeconds)
            return OffsetTimeHighPrecision(localTime, timeZone)
        }
    }

    override val hour: Int = localTime.hour
    override val minute: Int = localTime.minute
    override val decimalSecond: BigDecimal = localTime.decimalSecond

    override val elapsedSecond: BigDecimal by lazy {
        localTime.elapsedSecond
    }

    override fun plusHours(hours: Long): OffsetTimeHighPrecision {
        val timePassed = this.elapsedSecond
            .plus((hours * SECONDS_IN_HOUR).toBigDecimal())
            .let { normalizeElapsedTime(it) }
        return forSeconds(timePassed, timeZone)
    }

    override fun plusMinutes(minutes: Long): OffsetTimeHighPrecision {
        val timePassed = this.elapsedSecond
            .plus((minutes * DateTimeUtil.SECONDS_IN_MINUTE).toBigDecimal())
            .let { normalizeElapsedTime(it) }
        return forSeconds(timePassed, timeZone)
    }

    override fun plusSeconds(seconds: BigDecimal): OffsetTimeHighPrecision {
        val timePassed = this.elapsedSecond
            .plus(seconds)
            .let { normalizeElapsedTime(it) }
        return forSeconds(timePassed, timeZone)
    }

    override fun atDate(date: Date): OffsetTimestampHighPrecision =
        OffsetTimestampHighPrecision.forDateTime(date, this)

    override fun toTimeWithoutTimeZone(timeZone: TimeZone): LocalTimeHighPrecision =
        this.atTimeZone(timeZone).let {
            LocalTimeHighPrecision.of(it.hour, it.minute, it.decimalSecond)
        }

    override fun atTimeZone(timeZone: TimeZone): OffsetTimeHighPrecision =
        when (val valueTimeZone = this.timeZone) {
            TimeZone.UnknownTimeZone -> {
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> this
                    is TimeZone.UtcOffset -> of(
                        hour, minute, decimalSecond,
                        TimeZone.UtcOffset.of(0)
                    ).atTimeZone(timeZone)
                }
            }

            is TimeZone.UtcOffset -> {
                val utc = this.plusMinutes(-valueTimeZone.totalOffsetMinutes.toLong())
                when (timeZone) {
                    TimeZone.UnknownTimeZone -> of(utc.hour, utc.minute, utc.decimalSecond, timeZone)
                    is TimeZone.UtcOffset -> utc.plusMinutes(timeZone.totalOffsetMinutes.toLong())
                        .let { of(it.hour, it.minute, it.decimalSecond, timeZone) }
                }
            }
        }

    private fun normalizeElapsedTime(timePassed: BigDecimal): BigDecimal {
        val maxBD = BigDecimal.valueOf(SECONDS_IN_DAY)
        val remainder = timePassed % maxBD // % (remainder) in java may return negative value.
        return if (remainder < BigDecimal.ZERO) maxBD + remainder else remainder
    }
}
