package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.datetime.TimestampWithoutTimeZone
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A Timestamp without time zone value implementation.
 *
 * This implementation supports arbitrary precision upto system limit.
 */
internal class LocalTimestampHighPrecision private constructor(
    override val year: Int,
    override val month: Int,
    override val day: Int,
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal,
    _date: Date? = null,
    _time: TimeWithoutTimeZone? = null
) : TimestampWithoutTimeZone() {
    companion object {
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

        /**
         * Construct a timestamp without time zone value using its date component and time component.
         *
         * Notes the time component can not have time zone.
         */
        @JvmStatic
        fun forDateTime(date: Date, time: TimeWithoutTimeZone): LocalTimestampHighPrecision =
            LocalTimestampHighPrecision(
                date.year, date.month, date.day,
                time.hour, time.minute, time.decimalSecond.toBigDecimal(),
                date, time
            )
    }

    private val date: Date = _date ?: SqlDate.of(year, month, day)

    private val time: TimeWithoutTimeZone = _time ?: LocalTimeHighPrecision.of(hour, minute, decimalSecond)

    override fun plusYear(years: Long): TimestampWithoutTimeZone =
        forDateTime(this.date.plusYear(years), this.time)

    override fun plusMonths(months: Long): TimestampWithoutTimeZone =
        forDateTime(this.date.plusMonths(months), this.time)

    override fun plusDays(days: Long): TimestampWithoutTimeZone =
        forDateTime(this.date.plusDays(days), this.time)

    override fun plusHours(hours: Long): TimestampWithoutTimeZone {
        val rawHour = this.hour + hours
        val daysToCarry = if (rawHour >= 0) rawHour / 24 else rawHour / 24 - 1
        val afterHour = (rawHour % 24).toInt().let { if (it < 0) it + 24 else it }
        return forDateTime(date.plusDays(daysToCarry), LocalTimeHighPrecision.of(afterHour, minute, decimalSecond))
    }

    override fun plusMinutes(minutes: Long): TimestampWithoutTimeZone {
        val rawMinute = this.minute + minutes
        val hoursToCarry = if (rawMinute >= 0) rawMinute / 60 else rawMinute / 60 - 1
        val afterMinute = (rawMinute % 60).toInt().let { if (it < 0) it + 60 else it }
        return this.plusHours(hoursToCarry)
            .let { of(it.year, it.month, it.day, it.hour, afterMinute, it.decimalSecond) }
    }

    override fun plusSeconds(seconds: BigDecimal): TimestampWithoutTimeZone {
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

    override fun withTimeZone(timeZone: TimeZone): TimestampWithTimeZone =
        OffsetTimestampHighPrecision.of(
            year, month, day, hour, minute, decimalSecond, timeZone
        )
}
