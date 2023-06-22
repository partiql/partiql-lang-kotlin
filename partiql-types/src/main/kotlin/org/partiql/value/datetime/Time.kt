package org.partiql.value.datetime

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoField

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
        public fun of(
            hour: Int,
            minute: Int,
            second: BigDecimal,
            timeZone: TimeZone?,
            precision: Int?
        ): Time {
            try {
                val hour = ChronoField.HOUR_OF_DAY.checkValidValue(hour.toLong()).toInt()
                val minute = ChronoField.MINUTE_OF_HOUR.checkValidValue(minute.toLong()).toInt()
                // round down the second to check
                val wholeSecond = ChronoField.SECOND_OF_MINUTE.checkValidValue(second.setScale(0, RoundingMode.DOWN).toLong())
                val arbitraryTime = Time(hour, minute, second, timeZone, null)
                if (precision == null) {
                    return arbitraryTime
                }
                return arbitraryTime.toPrecision(precision)
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException("TIME", e.localizedMessage)
            } catch (e: IllegalStateException) {
                throw DateTimeException("TIME", e.localizedMessage)
            } catch (e: IllegalArgumentException) {
                throw DateTimeException("TIME", e.localizedMessage)
            }
        }
    }

    private fun toPrecision(precision: Int) =
        when {
            second.scale() == precision -> this
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
        val elapsedSeconds: BigDecimal =
            BigDecimal.valueOf(hour * SECONDS_IN_HOUR).add(BigDecimal.valueOf(minute * SECONDS_IN_MINUTE)).add(second)
        var rounded = elapsedSeconds.setScale(precision, RoundingMode.HALF_UP)
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
// public data class Time(
//    val hour: Int,
//    val minute: Int,
//    val second: BigDecimal,
//    val tzSign: Char?,
//    val tzHour: Int?,
//    val tzMinute: Int?,
//    val precision: Int? = null
// ) {
//    val hasUnknownTimeZone :Boolean = tzSign == '-' && tzHour == 0 && tzMinute == 0
//
//    val hasTimeZone : Boolean= tzSign != null
//
//    val elapsedSeconds: BigDecimal =
//        BigDecimal.valueOf(hour * SECONDS_IN_HOUR).add(BigDecimal.valueOf(minute * SECONDS_IN_MINUTE)).add(second)
//
//    public companion object {
//
//        // May throw exception, need to catch
//        // should move to parser
//        @Throws(DateTimeException::class)
//        public fun parseTimeLiteral(timeString: String): Time {
//            val matcher: Matcher = TIME_PATTERN.matcher(timeString)
//            if (!matcher.matches()) throw DateTimeException(
//                "TIME",
//                "TIME Format should be in HH-mm-ss.ddd+[+|-]hh:mm, received $timeString"
//            )
//            try {
//                val hour = ChronoField.HOUR_OF_DAY.checkValidValue(matcher.group("hour").toLong()).toInt()
//                val minute = ChronoField.MINUTE_OF_HOUR.checkValidValue(matcher.group("minute").toLong()).toInt()
//                val wholeSecond = ChronoField.SECOND_OF_MINUTE.checkValidValue(matcher.group("second").toLong())
//                val fractionPart = BigDecimal(".${matcher.group("fraction")}")
//                val second = BigDecimal.valueOf(wholeSecond).add(fractionPart)
//                val timezone = matcher.group("timezone") ?: null
//                if (timezone != null) {
//                    val (tzSign, tzHour, tzMinute) = getTimeZoneComponent(timezone)
//                    check(tzHour <= 23) {
//                        throw DateTimeException("TIME", "TIME ZONE HOUR should be less than 24")
//                    }
//                    check(tzMinute <= 59) {
//                        throw DateTimeException("TIME", "TIME ZONE MINUTE should be less than 60")
//                    }
//                    return Time(hour, minute, second, tzSign, tzHour, tzMinute, null)
//                }
//                return Time(hour, minute, second, null, null, null, null)
//            } catch (e: java.time.DateTimeException) {
//                throw DateTimeException("TIME", e.localizedMessage)
//            } catch (e: IllegalStateException) {
//                throw DateTimeException("TIME", e.localizedMessage)
//            } catch (e: IllegalArgumentException) {
//                throw DateTimeException("TIME", e.localizedMessage)
//            }
//        }
//
//        private fun getTimeZoneComponent(timezone: String) =
//            Triple(timezone.substring(0, 1).first(), timezone.substring(1, 3).toInt(), timezone.substring(4, 6).toInt())
//    }
//
//    private fun toPrecision(precision: Int) =
//        when {
//            second.scale() == precision -> this
//            second.scale() < precision -> paddingToPrecision(precision)
//            else -> roundToPrecision(precision)
//        }
//
//    private fun paddingToPrecision(precision: Int) =
//        Time(
//            this.hour,
//            this.minute,
//            this.second.setScale(precision),
//            this.tzSign,
//            this.tzHour,
//            this.tzMinute,
//            precision
//        )
//
//    private fun roundToPrecision(precision: Int): Time {
//        var rounded = this.elapsedSeconds.setScale(precision, RoundingMode.HALF_UP)
//        var newHours: Int = 0
//        var newMinutes: Int = 0
//        var newSeconds: BigDecimal = BigDecimal.ZERO
//        val secondsInHour = BigDecimal.valueOf(SECONDS_IN_HOUR)
//        val secondsInMin = BigDecimal.valueOf(SECONDS_IN_MINUTE)
//
//        if (rounded >= secondsInHour) {
//            val totalHours = rounded.divide(secondsInHour, 0, RoundingMode.DOWN)
//            rounded = rounded.subtract(totalHours.multiply(secondsInHour))
//            newHours = totalHours.intValueExact() % 24
//        }
//        if (rounded >= secondsInMin) {
//            val totalMinutes = rounded.divide(secondsInMin, 0, RoundingMode.DOWN)
//            rounded = rounded.subtract(totalMinutes.multiply(secondsInMin))
//            newMinutes = totalMinutes.intValueExact() % 60
//        }
//        newSeconds = rounded
//
//        return Time(newHours, newMinutes, newSeconds, this.tzSign, this.tzHour, this.tzMinute, precision)
//    }
// }
