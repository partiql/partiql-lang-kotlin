package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.math.max

internal class LocalTimestampLowPrecision private constructor(
    private val localDateTime: LocalDateTime,
    _year: Int? = null,
    _month: Int? = null,
    _day: Int? = null,
    _hour: Int? = null,
    _minute: Int? = null,
    _decimalSecond: BigDecimal? = null
) : TimestampWithoutTimeZone() {
    companion object {
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal
        ): LocalTimestampLowPrecision {
            val time = LocalTimeLowPrecision.of(hour, minute, decimalSecond)
            val localDateTime = LocalDateTime.of(
                year, month, day,
                time.hour, time.minute, time.second, time.nano
            )
            return LocalTimestampLowPrecision(localDateTime, year, month, day, hour, minute, decimalSecond)
        }

        @JvmStatic
        fun forDateTime(date: Date, time: LocalTimeLowPrecision): LocalTimestampLowPrecision {
            val localDateTime = LocalDateTime.of(
                date.year, date.month, date.day,
                time.hour, time.minute, time.second, time.nano
            )
            return LocalTimestampLowPrecision(localDateTime)
        }
    }

    override val year: Int = _year ?: localDateTime.year
    override val month: Int = _month ?: localDateTime.monthValue
    override val day: Int = _day ?: localDateTime.dayOfMonth
    override val hour: Int = _hour ?: localDateTime.hour
    override val minute: Int = _minute ?: localDateTime.minute
    override val decimalSecond: BigDecimal =
        _decimalSecond ?: BigDecimal.valueOf(localDateTime.second.toLong()).plus(BigDecimal.valueOf(localDateTime.nano.toLong(), 9))

    override fun plusYear(years: Long): TimestampWithoutTimeZone =
        localDateTime.plusYears(years).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusMonths(months: Long): TimestampWithoutTimeZone =
        localDateTime.plusMonths(months).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusDays(days: Long): TimestampWithoutTimeZone =
        localDateTime.plusDays(days).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusHours(hours: Long): TimestampWithoutTimeZone =
        localDateTime.plusHours(hours).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusMinutes(minutes: Long): TimestampWithoutTimeZone =
        localDateTime.plusMinutes(minutes).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusSeconds(seconds: BigDecimal): TimestampWithoutTimeZone =
        if (seconds.scale() > 9) {
            LocalTimestampHighPrecision.of(year, month, day, hour, minute, decimalSecond).plusSeconds(seconds)
        } else {
            val wholeSecond = seconds.setScale(0, RoundingMode.DOWN)
            val nano = seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            val newTime = localDateTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
            // the real precision of this operation, should be max(original_value.decimalSecond.precision, seconds.precision)
            val newDecimalSecond = newTime.second.toBigDecimal() + newTime.nano.toBigDecimal().movePointLeft(9)
            val roundedDecimalSecond = newDecimalSecond.setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(newTime.year, newTime.monthValue, newTime.dayOfMonth, newTime.hour, newTime.minute, roundedDecimalSecond)
        }

    override fun withTimeZone(timeZone: TimeZone): TimestampWithTimeZone =
        OffsetTimestampLowPrecision.of(year, month, day, hour, minute, decimalSecond, timeZone)
}
