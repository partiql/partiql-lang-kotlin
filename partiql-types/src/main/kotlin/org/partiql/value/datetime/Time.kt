package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_HOUR
import org.partiql.value.datetime.DateTimeUtil.SECONDS_IN_MINUTE
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoField
import kotlin.jvm.Throws

// TODO: Further break this down to tow implementation, one with nanosecond and below precision
//  and the other with nano-second and above precision, including arbitrary precision
//  The big decimal implementation is too slow and arguably for ion-compatibly reason only.
/**
 * This class is used to model both Time Without Time Zone type and Time With Time Zone Type.
 *
 * Informally, a data value of Time Without Time Zone represents a particular orientation of a clock
 * which will represent different instances of "time" based on the timezone.
 * a data value of Time With Time Zone represents an orientation of a clock attached with timezone offset.
 *
 */
public data class Time private constructor(
    val hour: Int,
    val minute: Int,
    val second: BigDecimal,
    val timeZone: TimeZone?,
    val precision: Int?
) {
    public companion object {
        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(
            hour: Int,
            minute: Int,
            second: BigDecimal,
            timeZone: TimeZone? = null,
            precision: Int? = null
        ): Time {
            try {
                ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong())
                ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong())
                // round down the second to check
                ChronoField.SECOND_OF_MINUTE.checkValidValue(second.setScale(0, RoundingMode.DOWN).toLong())
                val arbitraryTime = Time(hour, minute, second, timeZone, null)
                if (precision == null) { return arbitraryTime }
                return arbitraryTime.toPrecision(precision)
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage, e)
            }
        }
    }

    /**
     * Counting the time escaped from midnight 00:00:00 in seconds ( fraction included)
     */
    val elapsedSecond: BigDecimal by lazy {
        BigDecimal.valueOf(this.hour * SECONDS_IN_HOUR + this.minute * SECONDS_IN_MINUTE).plus(this.second)
    }

    private fun toPrecision(precision: Int) =
        when {
            second.scale() == precision -> this.copy(
                hour = hour,
                minute = minute,
                second = second,
                timeZone = timeZone,
                precision = precision
            )
            second.scale() < precision -> paddingToPrecision(precision)
            else -> roundToPrecision(precision)
        }

    private fun paddingToPrecision(precision: Int) =
        Time(
            this.hour,
            this.minute,
            this.second.setScale(precision),
            this.timeZone,
            precision
        )

    private fun roundToPrecision(precision: Int): Time {
        var rounded = this.elapsedSecond.setScale(precision, RoundingMode.HALF_UP)
        var newHours = 0
        var newMinutes = 0
        val secondsInHour = BigDecimal.valueOf(SECONDS_IN_HOUR)
        val secondsInMin = BigDecimal.valueOf(SECONDS_IN_MINUTE)

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

        return Time(newHours, newMinutes, rounded, this.timeZone, precision)
    }
}
