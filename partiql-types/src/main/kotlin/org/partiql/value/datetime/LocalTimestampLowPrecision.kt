package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

public data class LocalTimestampLowPrecision private constructor(
    val localDateTime: LocalDateTime
) : TimestampWithoutTimeZone {
    public companion object {
        @JvmStatic
        public fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal
        ): Timestamp {
            val date = SqlDate.of(year, month, day)
            val time = LocalTimeLowPrecision.of(hour, minute, decimalSecond)
            return forDateTime(date, time)
        }

        @JvmStatic
        public fun forDateTime(date: Date, time: LocalTimeLowPrecision) : Timestamp{
            val localDateTime = LocalDateTime.of(
                date.year, date.month, date.day,
                time.hour, time.minute, time.second, time.nanoOfSecond)
            return LocalTimestampLowPrecision(localDateTime)
        }
    }
    override val year: Int = localDateTime.year
    override val month: Int = localDateTime.monthValue
    override val day: Int = localDateTime.dayOfMonth
    override val hour: Int = localDateTime.hour
    override val minute: Int = localDateTime.minute
    override val decimalSecond: BigDecimal =
        BigDecimal.valueOf(localDateTime.second.toLong())
        .plus(BigDecimal.valueOf(localDateTime.nano.toLong(), 9))

    val second : Int = localDateTime.second
    val nano : Int = localDateTime.nano

    override fun plusYear(years: Long): Timestamp = LocalTimestampLowPrecision(localDateTime.plusYears(years))

    override fun plusMonths(months: Long): Timestamp = LocalTimestampLowPrecision(localDateTime.plusMonths(months))

    override fun plusDays(days: Long): Timestamp = LocalTimestampLowPrecision(localDateTime.plusDays(days))

    override fun plusHours(hours: Long): Timestamp = LocalTimestampLowPrecision(localDateTime.plusHours(hours))

    override fun plusMinutes(minutes: Long): Timestamp = LocalTimestampLowPrecision(localDateTime.plusMinutes(minutes))

    override fun plusSeconds(seconds: Number): Timestamp {
        val _seconds = seconds.toBigDecimal()
        if (_seconds.scale() > 9) {
            throw IllegalArgumentException("Second precision exceed nano second")
        }
        val wholeSecond = _seconds.setScale(0, RoundingMode.DOWN)
        val nano = _seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
        val newTime = localDateTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
        return LocalTimestampLowPrecision(newTime)
    }

    override fun toPrecision(precision: Int): Timestamp =
        when {
            decimalSecond.scale() == precision -> this
            decimalSecond.scale() < precision -> paddingToPrecision(precision)
            else -> roundToPrecision(precision)
        }

    private fun roundToPrecision(precision: Int): LocalTimestampLowPrecision {
        // if second fraction is 0.99999, precision 4
        // rounding this using half up will be 1.0000
        // diff is 0.0001
        // which means we need to add 0.0001 * 10^9 (100000)
        val decimalNano = decimalSecond.minus(this.second.toBigDecimal())
        val rounded = decimalNano.setScale(precision, RoundingMode.HALF_UP)
        val diff = rounded.minus(decimalNano).movePointRight(9).longValueExact()
        return LocalTimestampLowPrecision(localDateTime.plusNanos(diff))
    }

    private fun paddingToPrecision(precision: Int) =
        of(year, month, day, hour, minute, decimalSecond.setScale(precision))

    override fun toDate(): Date = SqlDate.of(year, month, day)

    override fun toTime(): Time = LocalTimeLowPrecision.of(hour, minute, decimalSecond)

}