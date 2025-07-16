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
    private val INTERVAL_SINGLE = Pattern.compile("(?<sign>[+-])?(?<is1>\\d+)(\\.(?<is2>\\d+))?")

    // Below are the regular expressions used for interval ranges (multiple datetime fields).
    private val INTERVAL_YEAR_MONTH = Pattern.compile("(?<sign>[+-])?(?<y>\\d+)-(?<m>\\d+)")
    private val INTERVAL_DAY_HOUR = Pattern.compile("(?<sign>[+-])?(?<d>\\d+)\\s+(?<h>\\d+)")
    private val INTERVAL_DAY_MINUTE = Pattern.compile("(?<sign>[+-])?(?<d>\\d+)\\s+(?<h>\\d+):(?<mi>\\d+)")
    private val INTERVAL_DAY_SECOND = Pattern.compile("(?<sign>[+-])?(?<d>\\d+)\\s+(?<h>\\d+):(?<mi>\\d+):(?<s1>\\d+)(\\.(?<s2>\\d+))?")
    private val INTERVAL_HOUR_MINUTE = Pattern.compile("(?<sign>[+-])?(?<h>\\d+):(?<mi>\\d+)")
    private val INTERVAL_HOUR_SECOND = Pattern.compile("(?<sign>[+-])?(?<h>\\d+):(?<mi>\\d+):(?<s1>\\d+)(\\.(?<s2>\\d+))?")
    private val INTERVAL_MINUTE_SECOND = Pattern.compile("(?<sign>[+-])?(?<mi>\\d+):(?<s1>\\d+)(\\.(?<s2>\\d+))?")

    private fun parseIntervalSingle(input: String, qualifier: IntervalQualifier.Single): Datum {
        val matcher: Matcher = INTERVAL_SINGLE.matcher(input)
        if (!matcher.matches()) {
            error("Invalid interval string: '$input'")
        }
        val sign: Int = if (matcher.group("sign") == "-") -1 else 1
        val integral: Int = matcher.group("is1")!!.toInt().times(sign)
        val fractional: Int? = matcher.group("is2")?.getNanosFromFractionalSeconds()?.times(sign)
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

    private class IntervalFieldExtractor(
        private val matcher: Matcher
    ) {
        private val sign: Int = if (matcher.group("sign") == "-") -1 else 1

        val year: Int by lazy { matcher.group("y")!!.toInt().times(sign) }
        val month: Int by lazy { matcher.group("m")!!.toInt().times(sign) }
        val day: Int by lazy { matcher.group("d")!!.toInt().times(sign) }
        val hour: Int by lazy { matcher.group("h")!!.toInt().times(sign) }
        val minute: Int by lazy { matcher.group("mi")!!.toInt().times(sign) }
        val second: Int by lazy { matcher.group("s1")!!.toInt().times(sign) }
        val nanos: Int by lazy {
            matcher.group("s2")?.getNanosFromFractionalSeconds()?.times(sign) ?: 0
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
                val extractor = IntervalFieldExtractor(matcher)
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalYearMonth(extractor.year, extractor.month, precision)
            }
            DatetimeField.DAY to DatetimeField.HOUR -> {
                val matcher: Matcher = INTERVAL_DAY_HOUR.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val extractor = IntervalFieldExtractor(matcher)
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalDayHour(extractor.day, extractor.hour, precision)
            }
            DatetimeField.DAY to DatetimeField.MINUTE -> {
                val matcher: Matcher = INTERVAL_DAY_MINUTE.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val extractor = IntervalFieldExtractor(matcher)
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalDayMinute(extractor.day, extractor.hour, extractor.minute, precision)
            }
            DatetimeField.DAY to DatetimeField.SECOND -> {
                val matcher: Matcher = INTERVAL_DAY_SECOND.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val extractor = IntervalFieldExtractor(matcher)
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                val scale = qualifier.endFieldFractionalPrecision ?: DEFAULT_INTERVAL_FRACTIONAL_PRECISION
                return Datum.intervalDaySecond(extractor.day, extractor.hour, extractor.minute, extractor.second, extractor.nanos, precision, scale)
            }
            DatetimeField.HOUR to DatetimeField.MINUTE -> {
                val matcher: Matcher = INTERVAL_HOUR_MINUTE.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val extractor = IntervalFieldExtractor(matcher)
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                return Datum.intervalHourMinute(extractor.hour, extractor.minute, precision)
            }
            DatetimeField.HOUR to DatetimeField.SECOND -> {
                val matcher: Matcher = INTERVAL_HOUR_SECOND.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val extractor = IntervalFieldExtractor(matcher)
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                val scale = qualifier.endFieldFractionalPrecision ?: DEFAULT_INTERVAL_FRACTIONAL_PRECISION
                return Datum.intervalHourSecond(extractor.hour, extractor.minute, extractor.second, extractor.nanos, precision, scale)
            }
            DatetimeField.MINUTE to DatetimeField.SECOND -> {
                val matcher: Matcher = INTERVAL_MINUTE_SECOND.matcher(input)
                if (!matcher.matches()) {
                    error("Invalid interval string: '$input'")
                }
                val extractor = IntervalFieldExtractor(matcher)
                val precision = qualifier.startFieldPrecision ?: DEFAULT_INTERVAL_PRECISION
                val scale = qualifier.endFieldFractionalPrecision ?: DEFAULT_INTERVAL_FRACTIONAL_PRECISION
                return Datum.intervalMinuteSecond(extractor.minute, extractor.second, extractor.nanos, precision, scale)
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
