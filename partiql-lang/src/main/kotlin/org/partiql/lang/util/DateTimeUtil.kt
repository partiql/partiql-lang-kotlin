package org.partiql.lang.util

import org.partiql.lang.errors.ErrorCode
import java.math.RoundingMode
import java.util.regex.Matcher
import java.util.regex.Pattern
import com.amazon.ion.Timestamp as IonTimestamp

internal object DateTimeUtil {

    // -------------------------------
    // |           COMMON            |
    // -------------------------------
    internal val DATETIME_PATTERN = Pattern.compile(
        "(?<year>[-+]?\\d{4,})-(?<month>\\d{1,2})-(?<day>\\d{1,2})" +
            "(?: (?<hour>\\d{1,2}):(?<minute>\\d{1,2})(?::(?<second>\\d{1,2})(?:\\.(?<fraction>\\d+))?)?)?" +
            "\\s*(?<timezone>[+-]\\d\\d:\\d\\d)?"
    )

    internal const val MILLIS_IN_SECOND: Long = 1000
    internal const val MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND
    internal const val MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE
    internal const val MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR

    // -------------------------------
    // |         TIMESTAMP           |
    // -------------------------------
    internal data class Timestamp(
        val ionTimestamp: IonTimestamp,
        val precision: Int?
    ) {
        companion object {
            internal fun parseTimestamp(value: String, precision: Int?): Timestamp {
                val matcher: Matcher = DATETIME_PATTERN.matcher(value)
                // TODO : CHANGE where we throw the error
                if (!matcher.matches()) ErrorCode.PARSE_INVALID_TIMESTAMP_STRING
                val year = matcher.group("year")
                val month = matcher.group("month")
                val day = matcher.group("day")
                val hour = matcher.group("hour")
                val minute = matcher.group("minute")
                val second = matcher.group("second")
                val fraction = matcher.group("fraction")

                val ionTimestamp = IonTimestamp.valueOf(
                    "$year-$month-${day}T$hour:$minute:$second.$fraction-00:00"
                )

                println(
                    """
                    Timestamp (no timezone)
                    year: $year, month: $month, day: $day, 
                    hour: $hour, minute: $minute, second: $second, nano: $fraction, 
                    precision: $precision
                    ionTimestamp: $ionTimestamp
                    """.trimIndent()
                )

                return when (precision) {
                    fraction.length, null -> Timestamp(ionTimestamp, precision)
                    else -> Timestamp(rescale(ionTimestamp, precision), precision)
                }
            }

            internal fun parseTimestampWithTimeZone(value: String, precision: Int?): Timestamp {
                val matcher: Matcher = DATETIME_PATTERN.matcher(value)
                if (!matcher.matches()) ErrorCode.PARSE_INVALID_TIMESTAMP_STRING
                try {
                    matcher.group("timezone")
                } catch (e: IllegalStateException) {
                    ErrorCode.PARSE_INVALID_TIMESTAMP_STRING
                }
                val year = matcher.group("year")
                val month = matcher.group("month")
                val day = matcher.group("day")
                val hour = matcher.group("hour")
                val minute = matcher.group("minute")
                val second = matcher.group("second")
                val fraction = matcher.group("fraction")
                val timezone = matcher.group("timezone")
                val ionTimestamp = IonTimestamp.valueOf(
                    "$year-$month-${day}T$hour:$minute:$second.$fraction$timezone"
                )

                println(
                    """
                    Timestamp With TimeZone
                    year: $year, month: $month, day: $day, 
                    hour: $hour, minute: $minute, second: $second, nano: $fraction, 
                    precision: $precision
                    ionTimestamp: $ionTimestamp
                    """.trimIndent()
                )
                return when (precision) {
                    fraction.length, null -> Timestamp(ionTimestamp, precision)
                    else -> Timestamp(rescale(ionTimestamp, precision), precision)
                }
            }

            /**
             * Rescale the timestamp object to desired precision
             */
            private fun rescale(ionTimestamp: IonTimestamp, precision: Int): IonTimestamp =
                IonTimestamp.forMillis(
                    ionTimestamp.decimalMillis.movePointLeft(3).setScale(precision, RoundingMode.HALF_UP).movePointRight(3),
                    ionTimestamp.localOffset
                )
        }
    }

    // -------------------------------
    // |     INTERVAL( DAY TIME)     |
    // -------------------------------
    internal data class IntervalDayTime(val milliSeconds: Long) {

        companion object {
            fun of(day: Long, hour: Long, minute: Long, second: Long, millis: Long): IntervalDayTime {
                val millis = toMillis(day, hour, minute, second, millis)
                return IntervalDayTime(millis)
            }

            private fun toMillis(day: Long, hour: Long, minute: Long, second: Long, millis: Long): Long =
                try {
                    var value = millis
                    value = Math.addExact(value, Math.multiplyExact(day, MILLIS_IN_DAY))
                    value = Math.addExact(value, Math.multiplyExact(hour, MILLIS_IN_HOUR))
                    value = Math.addExact(value, Math.multiplyExact(minute, MILLIS_IN_MINUTE))
                    value = Math.addExact(value, Math.multiplyExact(second, MILLIS_IN_SECOND))
                    value
                } catch (e: ArithmeticException) {
                    throw IllegalArgumentException(e)
                }
        }
    }

    // -------------------------------
    // |    INTERVAL( YEAR MONTH)    |
    // -------------------------------
    internal data class IntervalYearMonth(val months: Long) {
        companion object {
            fun of(year: Long, months: Long) {
                IntervalYearMonth(toMonth(year, months))
            }

            fun toMonth(year: Long, months: Long) = try {
                Math.addExact(months, Math.multiplyExact(year, months))
            } catch (e: ArithmeticException) {
                throw IllegalArgumentException(e)
            }
        }
    }
}
