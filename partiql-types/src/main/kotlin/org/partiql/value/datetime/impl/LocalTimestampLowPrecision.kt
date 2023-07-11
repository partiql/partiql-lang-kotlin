package org.partiql.value.datetime.impl

import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.math.max

/**
 * This implementation utilize [java.time.LocalDateTime] to handle timestamp without time zone if :
 * 1. The desired precision is below nanosecond (9 digits after decimal point).
 * 2. The desired timezone is within +18:00 to -18:00.
 *
 * The constructor functions assumes the above conditions to be true.
 */
internal class LocalTimestampLowPrecision private constructor(
    private val localDateTime: LocalDateTime,
    val date: SqlDate,
    val time: LocalTimeLowPrecision
) : TimestampWithoutTimeZone() {
    companion object {
        fun forDateTime(date: SqlDate, time: LocalTimeLowPrecision): LocalTimestampLowPrecision {
            val localDateTime = LocalDateTime.of(
                date.year, date.month, date.day,
                time.hour, time.minute, time.second, time.nano
            )
            return LocalTimestampLowPrecision(localDateTime, date, time)
        }
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal
        ): LocalTimestampLowPrecision {
            val date = SqlDate.of(year, month, day)
            val time = LocalTimeLowPrecision.of(hour, minute, decimalSecond)
            val localDateTime = LocalDateTime.of(
                year, month, day,
                time.hour, time.minute, time.second, time.nano
            )
            return LocalTimestampLowPrecision(localDateTime, date, time)
        }
    }

    override val year: Int = date.year
    override val month: Int = date.month
    override val day: Int = date.day
    override val hour: Int = time.hour
    override val minute: Int = time.minute
    override val decimalSecond: BigDecimal = time.decimalSecond

    override fun plusYear(years: Long): LocalTimestampLowPrecision =
        localDateTime.plusYears(years).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusMonths(months: Long): LocalTimestampLowPrecision =
        localDateTime.plusMonths(months).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusDays(days: Long): LocalTimestampLowPrecision =
        localDateTime.plusDays(days).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusHours(hours: Long): LocalTimestampLowPrecision =
        localDateTime.plusHours(hours).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusMinutes(minutes: Long): LocalTimestampLowPrecision =
        localDateTime.plusMinutes(minutes).let {
            of(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, decimalSecond)
        }

    override fun plusSeconds(seconds: BigDecimal): TimestampWithoutTimeZone =
        if (seconds.scale() > 9) {
            LocalTimestampHighPrecision.of(year, month, day, hour, minute, decimalSecond).plusSeconds(seconds)
        } else {
            val (wholeSecond, nano) = Utils.getSecondAndNanoFromDecimalSecond(seconds)
            val newTime = localDateTime.plusSeconds(wholeSecond).plusNanos(nano)
            // the real precision of this operation, should be max(original_value.decimalSecond.precision, seconds.precision)
            val newDecimalSecond = Utils.getDecimalSecondFromSecondAndNano(newTime.second.toLong(), newTime.nano.toLong())
            val roundedDecimalSecond = newDecimalSecond.setScale(max(this.decimalSecond.scale(), seconds.scale()), RoundingMode.UNNECESSARY)
            of(newTime.year, newTime.monthValue, newTime.dayOfMonth, newTime.hour, newTime.minute, roundedDecimalSecond)
        }

    override fun withTimeZone(timeZone: TimeZone): OffsetTimestampLowPrecision =
        OffsetTimestampLowPrecision.of(year, month, day, hour, minute, decimalSecond, timeZone)
}
