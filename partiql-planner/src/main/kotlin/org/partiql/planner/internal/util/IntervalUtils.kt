package org.partiql.planner.internal.util

import org.partiql.ast.DatetimeField
import org.partiql.ast.IntervalQualifier
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.util.regex.Matcher
import java.util.regex.Pattern

internal object IntervalUtils {

    /**
     * Parses an interval string into a [Datum] object, given a [qualifier].
     *
     * @param input The interval string to parse.
     * @param qualifier The interval qualifier.
     * @return A [Datum] object representing the interval.
     */
    internal fun parseInterval(input: String, qualifier: IntervalQualifier): Datum {
        return when (qualifier) {
            is IntervalQualifier.Single -> parseIntervalSingle(input, qualifier)
            is IntervalQualifier.Range -> parseIntervalRange(input, qualifier)
            else -> error("Unexpected IntervalQualifier JVM class: ${qualifier.javaClass.simpleName}")
        }
    }

    private const val DEFAULT_INTERVAL_PRECISION = 2
    private const val DEFAULT_INTERVAL_FRACTIONAL_PRECISION = 6

    /**
     * This represents an interval string unquoted used for parsing single datetime fields. "is2" is used for the optional
     * fractional part of seconds. We check this.
     */
    private val INTERVAL_SINGLE = Pattern.compile("(?<is1>\\d+)(\\.(?<is2>\\d+))?")

    // Below are the regular expressions used for interval ranges (multiple datetime fields).
    private val INTERVAL_YEAR_MONTH = Pattern.compile("(?<y>\\d+)-(?<m>\\d+)")
    private val INTERVAL_DAY_HOUR = Pattern.compile("(?<d>\\d+)\\s+(?<h>\\d+)")
    private val INTERVAL_DAY_MINUTE = Pattern.compile("(?<d>\\d+)\\s+(?<h>\\d+):(?<m>\\d+)")
    private val INTERVAL_DAY_SECOND = Pattern.compile("(?<d>\\d+)\\s+(?<h>\\d+):(?<m>\\d+):(?<s1>\\d+)(\\.(?<s2>\\d+))?")
    private val INTERVAL_HOUR_MINUTE = Pattern.compile("(?<h>\\d+):(?<m>\\d+)")
    private val INTERVAL_HOUR_SECOND = Pattern.compile("(?<h>\\d+):(?<m>\\d+):(?<s1>\\d+)(\\.(?<s2>\\d+))?")
    private val INTERVAL_MINUTE_SECOND = Pattern.compile("(?<m>\\d+):(?<s1>\\d+)(\\.(?<s2>\\d+))?")

    private fun parseIntervalSingle(input: String, qualifier: IntervalQualifier.Single): Datum {
        val matcher: Matcher = INTERVAL_SINGLE.matcher(input)
        if (!matcher.matches()) {
            error("Invalid interval string: '$input'")
        }
        val integral: Int = matcher.group("is1")!!.toInt()
        val fractional: Int? = matcher.group("is2")?.getNanosFromFractionalSeconds()
        val precision = qualifier.precision ?: DEFAULT_INTERVAL_PRECISION
        return when (qualifier.field.code()) {
            DatetimeField.YEAR -> {
                assertNullFractionalSeconds(fractional, qualifier.field)
                Datum.intervalYear(integral, precision)
            }
            DatetimeField.MONTH -> {
                assertNullFractionalSeconds(fractional, qualifier.field)
                Datum.intervalMonth(integral, precision)
            }
            DatetimeField.DAY -> {
                assertNullFractionalSeconds(fractional, qualifier.field)
                Datum.intervalDay(integral, precision)
            }
            DatetimeField.HOUR -> {
                assertNullFractionalSeconds(fractional, qualifier.field)
                Datum.intervalHour(integral, precision)
            }
            DatetimeField.MINUTE -> {
                assertNullFractionalSeconds(fractional, qualifier.field)
                Datum.intervalMinute(integral, precision)
            }
            DatetimeField.SECOND -> {
                val scale = qualifier.fractionalPrecision ?: DEFAULT_INTERVAL_FRACTIONAL_PRECISION
                when (fractional) {
                    null -> Datum.intervalSecond(integral, 0, precision, scale)
                    else -> Datum.intervalSecond(integral, fractional, precision, scale)
                }
            }
            else -> error("Could not parse interval string: $input with given qualifier: $qualifier")
        }
    }

    private fun parseIntervalRange(input: String, qualifier: IntervalQualifier.Range): Datum {
        val start = qualifier.startField.code()
        val end = qualifier.endField.code()
        when (start to end) {
            DatetimeField.YEAR to DatetimeField.MONTH -> {
                val matcher: Matcher = INTERVAL_YEAR_MONTH.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val year: Int = matcher.group("y")!!.toInt()
                val month: Int = matcher.group("m")!!.toInt()
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalYearMonth(year, month, precision)
            }
            DatetimeField.DAY to DatetimeField.HOUR -> {
                val matcher: Matcher = INTERVAL_DAY_HOUR.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val day: Int = matcher.group("d")!!.toInt()
                val hour: Int = matcher.group("h")!!.toInt()
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalDayHour(day, hour, precision)
            }
            DatetimeField.DAY to DatetimeField.MINUTE -> {
                val matcher: Matcher = INTERVAL_DAY_MINUTE.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val day: Int = matcher.group("d")!!.toInt()
                val hour: Int = matcher.group("h")!!.toInt()
                val minute: Int = matcher.group("m")!!.toInt()
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalDayMinute(day, hour, minute, precision)
            }
            DatetimeField.DAY to DatetimeField.SECOND -> {
                val matcher: Matcher = INTERVAL_DAY_SECOND.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val day: Int = matcher.group("d")!!.toInt()
                val hour: Int = matcher.group("h")!!.toInt()
                val minute: Int = matcher.group("m")!!.toInt()
                val second: Int = matcher.group("s1")!!.toInt()
                val nanos: Int = matcher.group("s2")?.getNanosFromFractionalSeconds() ?: 0
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                val scale = qualifier.endFieldFractionalPrecision ?: DEFAULT_INTERVAL_FRACTIONAL_PRECISION
                return Datum.intervalDaySecond(day, hour, minute, second, nanos, precision, scale)
            }
            DatetimeField.HOUR to DatetimeField.MINUTE -> {
                val matcher: Matcher = INTERVAL_HOUR_MINUTE.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val hour: Int = matcher.group("h")!!.toInt()
                val minute: Int = matcher.group("m")!!.toInt()
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalHourMinute(hour, minute, precision)
            }
            DatetimeField.HOUR to DatetimeField.SECOND -> {
                val matcher: Matcher = INTERVAL_HOUR_SECOND.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val hour: Int = matcher.group("h")!!.toInt()
                val minute: Int = matcher.group("m")!!.toInt()
                val second: Int = matcher.group("s1")!!.toInt()
                val nanos: Int = matcher.group("s2")?.getNanosFromFractionalSeconds() ?: 0
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                val scale = qualifier.endFieldFractionalPrecision ?: DEFAULT_INTERVAL_FRACTIONAL_PRECISION
                return Datum.intervalHourSecond(hour, minute, second, nanos, precision, scale)
            }
            DatetimeField.MINUTE to DatetimeField.SECOND -> {
                val matcher: Matcher = INTERVAL_MINUTE_SECOND.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val minute: Int = matcher.group("m")!!.toInt()
                val second: Int = matcher.group("s1")!!.toInt()
                val nanos: Int = matcher.group("s2")?.getNanosFromFractionalSeconds() ?: 0
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                val scale = qualifier.endFieldFractionalPrecision ?: DEFAULT_INTERVAL_FRACTIONAL_PRECISION
                return Datum.intervalMinuteSecond(minute, second, nanos, precision, scale)
            }
            else -> error("Not a valid interval range: ${qualifier.startField} TO ${qualifier.endField}.")
        }
    }

    private fun assertNullFractionalSeconds(fractionalSeconds: Int?, field: DatetimeField) {
        if (fractionalSeconds != null) {
            error("$field does not support fractional seconds.")
        }
    }

    private fun String.getNanosFromFractionalSeconds(): Int {
        val digits = 9
        if (this.length > digits) {
            error("Fractional seconds cannot be more than 9 digits.")
        }
        val fractionalSecondsBigDecimal = BigDecimal("0.$this")
        val newBigDecimal = fractionalSecondsBigDecimal.movePointRight(digits)
        val intBigDecimal = newBigDecimal.toInt()
        return intBigDecimal
    }
}
