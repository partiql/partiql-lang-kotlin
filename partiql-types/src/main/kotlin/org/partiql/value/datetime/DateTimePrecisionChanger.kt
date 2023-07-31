package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeValue.time
import org.partiql.value.datetime.DateTimeValue.timestamp
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime

internal object DateTimePrecisionChanger {

    fun toPrecision(precision: Int, dateTime: DateTime) =
        when (dateTime) {
            // this should not be called
            is DateImpl -> throw IllegalArgumentException("Date has no concept of precision")
            // making the compiler happy, and we don't need to use the double colon
            is Time -> {
                val scale = dateTime.decimalSecond.scale()
                when {
                    precision == scale -> dateTime
                    precision < scale -> roundToPrecision(precision, dateTime)
                    else -> paddingToPrecision(precision, dateTime)
                }
            }
            is Timestamp -> {
                val scale = dateTime.decimalSecond.scale()
                when {
                    precision == scale -> dateTime
                    precision < scale -> roundToPrecision(precision, dateTime)
                    else -> paddingToPrecision(precision, dateTime)
                }
            }
        }

    private fun paddingToPrecision(precision: Int, dateTime: DateTime): DateTime =
        when (dateTime) {
            is Time ->
                time(dateTime.hour, dateTime.minute, dateTime.decimalSecond.setScale(precision), dateTime.timeZone)
            is Timestamp ->
                timestamp(
                    dateTime.year, dateTime.month, dateTime.day,
                    dateTime.hour, dateTime.minute, dateTime.decimalSecond.setScale(precision), dateTime.timeZone
                )
            // Shall never be reached, making the compiler happy
            is DateImpl -> dateTime
        }

    private fun roundToPrecision(precision: Int, dateTime: DateTime): DateTime =
        when (dateTime) {
            // Shall never be reached, making the compiler happy
            is DateImpl -> dateTime
            is Time -> {
                if (isHighPrecision(dateTime.decimalSecond)) {
                    handleHighPrecisionRounding(dateTime, precision)
                } else {
                    handleLowPrecisionRounding(dateTime, precision)
                }
            }
            is Timestamp -> {
                val time = dateTime.toTime()
                val date = dateTime.toDate()
                val roundedTime = if (isHighPrecision(dateTime.decimalSecond)) {
                    handleHighPrecisionRounding(time, precision)
                } else {
                    handleLowPrecisionRounding(time, precision)
                }
                when ((time.elapsedSecond - roundedTime.elapsedSecond).abs() > BigDecimal.ONE) {
                    true -> timestamp(date.plusDays(1L), roundedTime)
                    false -> {
                        timestamp(date, roundedTime)
                    }
                }
            }
        }

    private val isHighPrecision: (decimal: BigDecimal) -> Boolean = { it.scale() > 9 }

    private fun handleHighPrecisionRounding(dateTime: Time, precision: Int): Time {
        var rounded = dateTime.elapsedSecond.setScale(precision, RoundingMode.HALF_UP)
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
        return time(newHours, newMinutes, rounded, dateTime.timeZone)
    }

    private fun handleLowPrecisionRounding(dateTime: Time, precision: Int): Time {
        val wholeSecondBD = dateTime.decimalSecond.setScale(0, RoundingMode.DOWN)
        val decimalNano = dateTime.decimalSecond.minus(wholeSecondBD)
        val wholeSecond = wholeSecondBD.intValueExact()
        val nano = decimalNano.movePointRight(9).intValueExact()
        val rounded = decimalNano.setScale(precision, RoundingMode.HALF_UP).movePointRight(9).intValueExact()
        val diff = rounded - nano
        val originalLocalTime = LocalTime.of(dateTime.hour, dateTime.minute, wholeSecond, nano)
        val newDateTime = originalLocalTime.plusNanos(diff.toLong())
        val newDecimalSecond = newDateTime.nano.toBigDecimal().movePointLeft(9).stripTrailingZeros().setScale(precision) + newDateTime.second.toBigDecimal()
        return time(newDateTime.hour, newDateTime.minute, newDecimalSecond, dateTime.timeZone)
    }
}
