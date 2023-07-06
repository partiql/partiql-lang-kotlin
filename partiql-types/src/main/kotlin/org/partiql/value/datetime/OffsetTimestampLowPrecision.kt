package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.jvm.Throws

public data class OffsetTimestampLowPrecision(
    val offsetDateTime: OffsetDateTime,
    val isUnknownTimeZone: Boolean
) : TimestampWithTimeZone{
    public companion object {
        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            timeZone: TimeZone
        ): OffsetTimestampLowPrecision {
            try {
                return when (timeZone) {
                    TimeZone.UnknownTimeZone ->
                        OffsetTimestampLowPrecision(
                            OffsetDateTime.of(year, month, day, hour, minute, second, nanoOfSecond, ZoneOffset.UTC),
                            true
                        )

                    is TimeZone.UtcOffset -> OffsetTimestampLowPrecision(
                        OffsetDateTime.of(
                            year,month, day,
                            hour, minute, second, nanoOfSecond,
                            ZoneOffset.ofTotalSeconds(timeZone.totalOffsetMinutes * 60)
                        ),
                        false
                    )
                }
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage)
            }
        }

        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(year: Int, month: Int, day: Int, hour: Int, minute: Int, decimalSecond: BigDecimal, timeZone: TimeZone): OffsetTimestampLowPrecision {
            if (decimalSecond.scale() > 9) {
                throw DateTimeException("Second precision exceed nano second")
            }
            val wholeSecond = decimalSecond.setScale(0, RoundingMode.DOWN)
            val nano = decimalSecond.minus(wholeSecond).let { it.movePointRight(it.scale()) }
            return of(year, month, day, hour, minute, wholeSecond.intValueExact(), nano.intValueExact(), timeZone)
        }
    }
    override val timeZone: TimeZone =
        if (isUnknownTimeZone) TimeZone.UnknownTimeZone else TimeZone.UtcOffset.of(offsetDateTime.offset.totalSeconds / 60)


    override val year: Int = offsetDateTime.year
    override val month: Int = offsetDateTime.monthValue
    override val day: Int = offsetDateTime.dayOfMonth
    override val hour: Int = offsetDateTime.hour
    override val minute: Int = offsetDateTime.minute
    override val decimalSecond: BigDecimal = BigDecimal.valueOf(offsetDateTime.second.toLong())
            .plus(BigDecimal.valueOf(offsetDateTime.nano.toLong(), 9))

    val second: Int = offsetDateTime.second
    val nano: Int = offsetDateTime.nano

    override fun plusYear(years: Long): Timestamp = OffsetTimestampLowPrecision(offsetDateTime.plusYears(years), isUnknownTimeZone)

    override fun plusMonths(months: Long): Timestamp = OffsetTimestampLowPrecision(offsetDateTime.plusMonths(months), isUnknownTimeZone)

    override fun plusDays(days: Long): Timestamp = OffsetTimestampLowPrecision(offsetDateTime.plusDays(days), isUnknownTimeZone)

    override fun plusHours(hours: Long): Timestamp = OffsetTimestampLowPrecision(offsetDateTime.plusHours(hours), isUnknownTimeZone)

    override fun plusMinutes(minutes: Long): Timestamp = OffsetTimestampLowPrecision(offsetDateTime.plusMinutes(minutes), isUnknownTimeZone)

    override fun plusSeconds(seconds: Number): Timestamp {
        val _seconds = seconds.toBigDecimal()
        if (_seconds.scale() > 9) {
            throw IllegalArgumentException("Second precision exceed nano second")
        }
        val wholeSecond = _seconds.setScale(0, RoundingMode.DOWN)
        val nano = _seconds.minus(wholeSecond).let { it.movePointRight(it.scale()) }
        val newTime = offsetDateTime.plusSeconds(wholeSecond.longValueExact()).plusNanos(nano.longValueExact())
        return OffsetTimestampLowPrecision(newTime, isUnknownTimeZone)
    }

    override fun toPrecision(precision: Int): Timestamp =
        when {
            decimalSecond.scale() == precision -> this
            decimalSecond.scale() < precision -> paddingToPrecision(precision)
            else -> roundToPrecision(precision)
        }

    private fun roundToPrecision(precision: Int): OffsetTimestampLowPrecision {
        // if second fraction is 0.99999, precision 4
        // rounding this using half up will be 1.0000
        // diff is 0.0001
        // which means we need to add 0.0001 * 10^9 (100000)
        val decimalNano = decimalSecond.minus(this.second.toBigDecimal())
        val rounded = decimalNano.setScale(precision, RoundingMode.HALF_UP)
        val diff = rounded.minus(decimalNano).movePointRight(9).longValueExact()
        return OffsetTimestampLowPrecision(offsetDateTime.plusNanos(diff), this.isUnknownTimeZone)
    }

    private fun paddingToPrecision(precision: Int) =
        of(year, month, day, hour, minute, decimalSecond.setScale(precision), timeZone)


    override fun atTimeZone(timeZone: TimeZone): Timestamp {
        TODO("Not yet implemented")
    }

    override fun toDate(): Date = SqlDate.of(year, month, day)

    override fun toTime(): Time = OffsetTimeLowPrecision.of(hour, minute, decimalSecond, timeZone)
}