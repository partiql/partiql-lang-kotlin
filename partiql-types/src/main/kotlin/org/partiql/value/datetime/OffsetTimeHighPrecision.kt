package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoField
import kotlin.jvm.Throws

public data class OffsetTimeHighPrecision private constructor(
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal,
    override val timeZone: TimeZone
) : TimeWithTimeZone {

    public companion object {
        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(
            hour: Int,
            minute: Int,
            second: BigDecimal,
            timeZone: TimeZone
        ): OffsetTimeHighPrecision {
            try {
                ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong())
                ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong())
                // round down the decimalSecond to check
                ChronoField.SECOND_OF_MINUTE.checkValidValue(second.setScale(0, RoundingMode.DOWN).toLong())
                return OffsetTimeHighPrecision(hour, minute, second, timeZone)
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage, e)
            }
        }

        public fun forSeconds(second: BigDecimal, timeZone: TimeZone, precision: Int? = null): OffsetTimeHighPrecision {
            val wholeSecond = second.longValueExact()
            val fraction = second.minus(BigDecimal.valueOf(wholeSecond))
            var total = wholeSecond
            val hour = total / DateTimeUtil.SECONDS_IN_HOUR
            total -= hour * DateTimeUtil.SECONDS_IN_HOUR
            val minute = total / DateTimeUtil.SECONDS_IN_MINUTE
            total -= minute * DateTimeUtil.SECONDS_IN_MINUTE
            return of(hour.toInt(), minute.toInt(), fraction.plus(BigDecimal.valueOf(total)), timeZone)
        }
    }

    /**
     * Counting the time escaped from midnight 00:00:00 in seconds ( fraction included)
     */
    val elapsedSecond: BigDecimal by lazy {
        BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE).plus(this.decimalSecond)
    }

    public override fun toPrecision(precision: Int): OffsetTimeHighPrecision =
        when {
            decimalSecond.scale() == precision -> this.copy(
                hour = hour,
                minute = minute,
                decimalSecond = decimalSecond,
                timeZone = timeZone
            )
            decimalSecond.scale() < precision -> paddingToPrecision(precision)
            else -> roundToPrecision(precision)
        }

    override fun atDate(date: Date): Timestamp =
        OffsetTimestampHighPrecision.forDateTime(date, this)


    override fun plusHours(hours: Long): OffsetTimeHighPrecision =
        forSeconds(this.elapsedSecond.plus((hours * SECONDS_IN_HOUR).toBigDecimal()), timeZone)


    override fun plusMinutes(minutes: Long): OffsetTimeHighPrecision =
        forSeconds(
            this.elapsedSecond.plus((minutes * DateTimeUtil.SECONDS_IN_MINUTE).toBigDecimal()),
            timeZone
        )

    override fun plusSeconds(seconds: Number): OffsetTimeHighPrecision =
        forSeconds(this.elapsedSecond.plus(seconds.toBigDecimal()), timeZone)

    override fun atTimeZone(timeZone: TimeZone): OffsetTimeHighPrecision =  when (this.timeZone) {
        TimeZone.UnknownTimeZone -> {
            when (timeZone) {
                TimeZone.UnknownTimeZone -> this
                is TimeZone.UtcOffset -> this.copy(timeZone = TimeZone.UtcOffset.of(0)).atTimeZone(timeZone)
            }
        }

        is TimeZone.UtcOffset -> {
            val utc = this.plusMinutes(-this.timeZone.totalOffsetMinutes.toLong())
            when (timeZone) {
                TimeZone.UnknownTimeZone -> utc.copy(timeZone = timeZone)
                is TimeZone.UtcOffset -> utc.plusMinutes(timeZone.totalOffsetMinutes.toLong()).copy(timeZone = timeZone)
            }
        }
    }

    private fun paddingToPrecision(precision: Int) =
        OffsetTimeHighPrecision(
            this.hour,
            this.minute,
            this.decimalSecond.setScale(precision),
            this.timeZone
        )

    private fun roundToPrecision(precision: Int): OffsetTimeHighPrecision {
        var rounded = this.elapsedSecond.setScale(precision, RoundingMode.HALF_UP)
        var newHours = 0
        var newMinutes = 0
        val secondsInHour = BigDecimal.valueOf(DateTimeUtil.SECONDS_IN_HOUR)
        val secondsInMin = BigDecimal.valueOf(DateTimeUtil.SECONDS_IN_MINUTE)

        if (rounded >= secondsInHour) {
            val totalHours = rounded.divide(secondsInHour, 0, RoundingMode.DOWN)
            rounded = rounded.subtract(totalHours.multiply(secondsInHour))
            newHours = totalHours.intValueExact() % 24
        }
        if (rounded >= secondsInMin) {
            val totalMinutes = rounded.divide(secondsInMin, 0, RoundingMode.DOWN)
            rounded = rounded.subtract(totalMinutes.multiply(secondsInMin))
            newMinutes = totalMinutes.intValueExact() % 60
        }

        return OffsetTimeHighPrecision(newHours, newMinutes, rounded, this.timeZone)
    }
}