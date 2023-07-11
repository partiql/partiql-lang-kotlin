package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.JAVA_MAX_OFFSET
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.impl.LocalTimeHighPrecision
import org.partiql.value.datetime.impl.LocalTimeLowPrecision
import org.partiql.value.datetime.impl.LocalTimestampHighPrecision
import org.partiql.value.datetime.impl.LocalTimestampLowPrecision
import org.partiql.value.datetime.impl.OffsetTimeHighPrecision
import org.partiql.value.datetime.impl.OffsetTimeLowPrecision
import org.partiql.value.datetime.impl.OffsetTimestampHighPrecision
import org.partiql.value.datetime.impl.OffsetTimestampLowPrecision
import org.partiql.value.datetime.impl.SqlDate
import java.math.BigDecimal
import kotlin.math.absoluteValue

public object DateTimeValue {

    public val nowZ: TimestampWithTimeZone = OffsetTimestampLowPrecision.nowZ()

    @JvmOverloads
    public fun timestamp(
        year: Int,
        month: Int = 1,
        day: Int = 1,
        hour: Int = 0,
        minute: Int = 0,
        second: BigDecimal = BigDecimal.ZERO,
        timeZone: TimeZone? = null
    ): Timestamp =
        when (timeZone) {
            TimeZone.UnknownTimeZone -> {
                if (second.scale() <= 9) {
                    OffsetTimestampLowPrecision.of(year, month, day, hour, minute, second, timeZone)
                } else {
                    OffsetTimestampHighPrecision.of(year, month, day, hour, minute, second, timeZone)
                }
            }
            is TimeZone.UtcOffset -> {
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

    @JvmOverloads
    public fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        timeZone: TimeZone? = null
    ): Timestamp =
        timestamp(year, month, day, hour, minute, second.toBigDecimal(), timeZone)

    public fun timestamp(date: Date, time: Time): Timestamp =
        when (time) {
            is TimeWithTimeZone -> timestamp(
                date.year,
                date.month,
                date.day,
                time.hour,
                time.minute,
                time.decimalSecond,
                time.timeZone
            )

            is TimeWithoutTimeZone -> timestamp(
                date.year,
                date.month,
                date.day,
                time.hour,
                time.minute,
                time.decimalSecond
            )
        }

    public fun timestamp(ionTimestamp: com.amazon.ion.Timestamp): TimestampWithTimeZone =
        if (ionTimestamp.decimalSecond.scale() <= 9) {
            OffsetTimestampLowPrecision.forIonTimestamp(ionTimestamp)
        } else {
            OffsetTimestampHighPrecision.forIonTimestamp(ionTimestamp)
        }

    public fun timestamp(epochSeconds: BigDecimal, timeZone: TimeZone): TimestampWithTimeZone =
        if (epochSeconds.scale() <= 9) {
            OffsetTimestampLowPrecision.forEpochSeconds(epochSeconds, timeZone)
        } else {
            OffsetTimestampHighPrecision.forEpochSeconds(epochSeconds, timeZone)
        }

    @JvmOverloads
    public fun time(
        hour: Int,
        minute: Int,
        second: BigDecimal,
        timeZone: TimeZone? = null
    ): Time =
        when (timeZone) {
            TimeZone.UnknownTimeZone -> {
                if (second.scale() <= 9) {
                    OffsetTimeLowPrecision.of(hour, minute, second, timeZone)
                } else {
                    OffsetTimeHighPrecision.of(hour, minute, second, timeZone)
                }
            }
            is TimeZone.UtcOffset -> {
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

    @JvmOverloads
    public fun time(
        hour: Int,
        minute: Int,
        second: Int,
        timeZone: TimeZone? = null
    ): Time =
        time(hour, minute, second.toBigDecimal(), timeZone)

    @JvmOverloads
    public fun time(
        hour: Int,
        minute: Int,
        second: Int,
        nano: Int,
        timeZone: TimeZone? = null
    ): Time {
        val decimalSecond = second.toBigDecimal().plus(nano.toBigDecimal().movePointLeft(9))
        return time(hour, minute, decimalSecond, timeZone)
    }

    @JvmStatic
    public fun date(
        year: Int,
        month: Int,
        day: Int
    ): Date =
        SqlDate.of(year, month, day)
}
