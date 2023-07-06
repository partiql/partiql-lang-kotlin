package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A Timestamp without time zone value implementation.
 *
 * This implementation supports arbitrary precision upto system limit.
 */
public data class LocalTimestampHighPrecision(
    override val year: Int,
    override val month: Int,
    override val day: Int,
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal
) : TimestampWithoutTimeZone {
    public companion object {
        /**
         * Construct a timestamp value using date time field and a given precision.
         *
         * @param year Year field
         * @param month Month field
         * @param day Day field
         * @param hour Hour field
         * @param second Second field, include fraction decimalSecond
         */
        public fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            decimalSecond: BigDecimal
        ): Timestamp {
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
        public fun forDateTime(date: Date, time: TimeWithoutTimeZone): LocalTimestampHighPrecision =
            LocalTimestampHighPrecision(
                date.year, date.month, date.day,
                time.hour, time.minute, time.decimalSecond.toBigDecimal()
            )
    }

    public val date: Date = SqlDate.of(year, month, day)

    public val time: LocalTimeHighPrecision = LocalTimeHighPrecision.of(hour, minute, decimalSecond)

    // Operation
    override fun plusYear(years: Long): LocalTimestampHighPrecision = forDateTime(this.date.plusYear(years), this.time)

    override fun plusMonths(months: Long): LocalTimestampHighPrecision =
        forDateTime(this.date.plusMonths(months), this.time)

    override fun plusDays(days: Long): LocalTimestampHighPrecision = forDateTime(this.date.plusDays(days), this.time)

    override fun plusHours(hours: Long): LocalTimestampHighPrecision {
        val rawHour = this.hour + hours
        val daysToCarry = if (rawHour >= 0) rawHour / 24 else rawHour / 24 - 1
        val afterHour = (rawHour % 24).toInt().let { if (it < 0) it + 24 else it }
        return plusDays(daysToCarry).copy(hour = afterHour)
    }

    override fun plusMinutes(minutes: Long): LocalTimestampHighPrecision {
        val rawMinute = this.minute + minutes
        val hoursToCarry = if (rawMinute >= 0) rawMinute / 60 else rawMinute / 60 - 1
        val afterMinute = (rawMinute % 60).toInt().let { if (it < 0) it + 60 else it }
        return this.plusHours(hoursToCarry).copy(minute = afterMinute)
    }

    override fun plusSeconds(seconds: Number): LocalTimestampHighPrecision {
        val _seconds = seconds.toBigDecimal()
        val rawSecond = decimalSecond.plus(_seconds)
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
        return this.plusMonths(minutesToCarry).copy(decimalSecond = afterSecond)
    }

    override fun toPrecision(precision: Int): Timestamp {
        val roundedTime = this.time.toPrecision(precision)
        // if the rounding result and the original result differs in more than 1 decimalSecond, then we need to carry to date
        return when ((this.time.elapsedSecond - roundedTime.elapsedSecond).abs() > BigDecimal.ONE) {
            true -> forDateTime(date.plusDays(1L), roundedTime)
            false -> {
                forDateTime(date, roundedTime)
            }
        }
    }

    override fun toDate(): Date = SqlDate.of(year, month, day)

    override fun toTime(): Time = LocalTimeHighPrecision.of(hour, minute, decimalSecond)
}