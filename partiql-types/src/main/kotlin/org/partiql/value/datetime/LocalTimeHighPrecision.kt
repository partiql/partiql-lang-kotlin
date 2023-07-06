package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoField
import kotlin.jvm.Throws

public data class LocalTimeHighPrecision private constructor(
    override val hour: Int,
    override val minute: Int,
    override val decimalSecond: BigDecimal
) : TimeWithoutTimeZone {
    public companion object {
        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(
            hour: Int,
            minute: Int,
            second: BigDecimal
        ): LocalTimeHighPrecision {
            try {
                ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong())
                ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong())
                // round down the decimalSecond to check
                ChronoField.SECOND_OF_MINUTE.checkValidValue(second.setScale(0, RoundingMode.DOWN).toLong())
                return LocalTimeHighPrecision(hour, minute, second)
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage, e)
            }
        }

        public fun forSeconds(second: BigDecimal): LocalTimeHighPrecision {
            val wholeSecond = second.longValueExact()
            val fraction = second.minus(BigDecimal.valueOf(wholeSecond))
            var total = wholeSecond
            val hour = total / DateTimeUtil.SECONDS_IN_HOUR
            total -= hour * DateTimeUtil.SECONDS_IN_HOUR
            val minute = total / DateTimeUtil.SECONDS_IN_MINUTE
            total -= minute * DateTimeUtil.SECONDS_IN_MINUTE
            return of(hour.toInt(), minute.toInt(), fraction.plus(BigDecimal.valueOf(total)))
        }
    }

    /**
     * Counting the time escaped from midnight 00:00:00 in seconds ( fraction included)
     */
    val elapsedSecond: BigDecimal by lazy {
        BigDecimal.valueOf(this.hour * DateTimeUtil.SECONDS_IN_HOUR + this.minute * DateTimeUtil.SECONDS_IN_MINUTE).plus(this.decimalSecond)
    }

    // Operations
    override fun plusHours(hours: Long): TimeWithoutTimeZone =
        forSeconds(
            this.elapsedSecond.plus((hours * DateTimeUtil.SECONDS_IN_HOUR).toBigDecimal())
        )

    override fun plusMinutes(minutes: Long): TimeWithoutTimeZone =
        forSeconds(
            this.elapsedSecond.plus((minutes * DateTimeUtil.SECONDS_IN_MINUTE).toBigDecimal())
        )


    override fun plusSeconds(seconds: Number): TimeWithoutTimeZone =
        forSeconds(this.elapsedSecond.plus(seconds.toBigDecimal()))

    override fun atDate(date: Date): Timestamp = LocalTimestampHighPrecision.forDateTime(date, this)

    // private
    public override fun toPrecision(precision: Int): LocalTimeHighPrecision =
        when {
            decimalSecond.scale() == precision -> this
            decimalSecond.scale() < precision -> paddingToPrecision(precision)
            else -> roundToPrecision(precision)
        }

    private fun paddingToPrecision(precision: Int) =
        LocalTimeHighPrecision(
            this.hour,
            this.minute,
            this.decimalSecond.setScale(precision)
        )

    private fun roundToPrecision(precision: Int):  LocalTimeHighPrecision{
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

        return LocalTimeHighPrecision(newHours, newMinutes, rounded)
    }
}