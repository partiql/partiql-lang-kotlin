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

package org.partiql.value.util

import org.partiql.spi.value.Datum
import org.partiql.value.datetime.Date
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import org.partiql.types.PType
import org.partiql.value.datetime.impl.LocalTimeLowPrecision
import org.partiql.value.datetime.impl.LocalTimeLowPrecision.Companion.forNano
import org.partiql.value.datetime.impl.LocalTimestampLowPrecision.Companion.forDateTime
import org.partiql.value.datetime.impl.OffsetTimeLowPrecision
import org.partiql.value.datetime.impl.OffsetTimeLowPrecision.Companion.of
import org.partiql.value.datetime.impl.OffsetTimestampLowPrecision.Companion.forDateTime
import org.partiql.value.datetime.impl.SqlDate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset

internal object DateTimeUtil {

    internal const val MILLIS_IN_SECOND: Long = 1000
    internal const val MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND
    internal const val MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE
    internal const val MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR
    internal const val SECONDS_IN_MINUTE = 60L
    internal const val SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE
    internal const val SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR
    internal const val MAX_TIME_ZONE_HOURS: Int = 23
    internal const val MAX_TIME_ZONE_MINUTES: Int = 59
    internal const val MAX_TOTAL_OFFSET_MINUTES: Int = MAX_TIME_ZONE_HOURS * 60 + MAX_TIME_ZONE_MINUTES
    internal const val NANOS_IN_SECOND: Long = 1_000_000_000L
    internal const val JAVA_MAX_OFFSET: Int = 18 * 60 // java offset valid range -18:00 to 18:00

    // In date time, we should only concern with BigDecimal, Int, and Long
    internal fun Number.toBigDecimal(): BigDecimal = when (this) {
        is BigDecimal -> this
        is Long -> BigDecimal.valueOf(this)
        is Int -> BigDecimal.valueOf(this.toLong())
        else -> throw IllegalArgumentException("can not convert $this to BigDecimal")
    }

    @JvmStatic
    fun toDate(date: LocalDate): Date {
        return SqlDate.of(date.year, date.monthValue, date.dayOfMonth)
    }

    @JvmStatic
    fun toTime(time: LocalTime): Time {
        return forNano(time.hour, time.minute, time.second, time.nano)
    }

    @JvmStatic
    fun toTime(time: OffsetTime): Time {
        val offset = time.offset.totalSeconds
        val zone: TimeZone = TimeZone.UtcOffset.of(offset / 60, offset % 60)
        return of(time.hour, time.minute, time.second, time.nano, zone)
    }

    @JvmStatic
    fun toTimestamp(timestamp: LocalDateTime): Timestamp {
        val date = toDate(timestamp.toLocalDate())
        val time = toTime(timestamp.toLocalTime())
        return forDateTime(
            (date as SqlDate), (time as LocalTimeLowPrecision)
        )
    }

    @JvmStatic
    fun toTimestamp(timestamp: OffsetDateTime): Timestamp {
        val date = toDate(timestamp.toLocalDate())
        val time = toTime(timestamp.toOffsetTime())
        return forDateTime(
            date, (time as OffsetTimeLowPrecision)
        )
    }

    @JvmStatic
    fun toDatumDate(date: Date): Datum {
        return Datum.date(LocalDate.of(date.year, date.month, date.day))
    }

    @JvmStatic
    fun toDatumTime(time: Time): Datum {
        // [0-59].000_000_000
        val ds = time.decimalSecond
        val second: Int = ds.toInt()
        val nanoOfSecond: Int = ds.remainder(BigDecimal.ONE).movePointRight(9).toInt()
        // local
        val local = LocalTime.of(time.hour, time.minute, second, nanoOfSecond)
        // check offset
        if (time.timeZone != null && time.timeZone is TimeZone.UtcOffset) {
            val zone = time.timeZone as TimeZone.UtcOffset
            val offset = ZoneOffset.ofHoursMinutes(zone.tzHour, zone.tzMinute)
            return Datum.timez(local.atOffset(offset), 9)
        }
        return Datum.time(local, 9)
    }

    @JvmStatic
    fun toDatumTimestamp(timestamp: Timestamp): Datum {
        val date = toDatumDate(timestamp.toDate()).localDate
        val time = toDatumTime(timestamp.toTime())
        return when (time.type.code()) {
            PType.TIME -> Datum.timestamp(LocalDateTime.of(date, time.localTime), 9)
            PType.TIMEZ -> Datum.timestampz(OffsetDateTime.of(date, time.localTime, time.offsetTime.offset), 9)
            else -> throw IllegalArgumentException("unsupported timestamp type")
        }
    }
}
