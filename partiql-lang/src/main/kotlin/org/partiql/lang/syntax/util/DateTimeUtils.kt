package org.partiql.lang.syntax.util

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.regex.Matcher
import java.util.regex.Pattern

internal object DateTimeUtils {
    val DATE_PATTERN = Pattern.compile("(?<year>\\d{4,})-(?<month>\\d{2,})-(?<day>\\d{2,})")
    val TIME_PATTERN = Pattern.compile("(?<hour>\\d{2,}):(?<minute>\\d{2,}):(?<second>\\d{2,})(?:\\.(?<fraction>\\d+))?\\s*(?<timezone>([+-]\\d\\d:\\d\\d)|(?<utc>[Zz]))?")
    val SQL_TIMESTAMP_DATE_TIME_DELIMITER = "\\s+".toRegex()
    val RFC8889_TIMESTAMP_DATE_TIME_DELIMITER = "[Tt]".toRegex()
    val TIMESTAMP_PATTERN = "(?<date>$DATE_PATTERN)($SQL_TIMESTAMP_DATE_TIME_DELIMITER|$RFC8889_TIMESTAMP_DATE_TIME_DELIMITER)(?<time>$TIME_PATTERN)".toRegex().toPattern()

    fun parseDateLiteral(dateString: String): Date {
        val matcher: Matcher = DATE_PATTERN.matcher(dateString)
        if (!matcher.matches()) throw DateTimeException(
            "DATE",
            "Date Format should be in YYYY-MM-DD, received $dateString"
        )
        val year = matcher.group("year").toInt()
        val month = matcher.group("month").toInt()
        val day = matcher.group("day").toInt()
        // Validate the date using Local date, so we don't have to write custom validation logic
        try {
            LocalDate.of(year, month, day)
        } catch (e: java.time.DateTimeException) {
            throw DateTimeException("Date", e.localizedMessage)
        }
        return Date.of(year, month, day)
    }

    fun parseTimeLiteral(timeString: String): Time {
        val matcher: Matcher = TIME_PATTERN.matcher(timeString)
        if (!matcher.matches()) throw DateTimeException(
            "TIME",
            "TIME Format should be in HH-mm-ss.ddd+[+|-]hh:mm, received $timeString"
        )
        try {
            val hour = ChronoField.HOUR_OF_DAY.checkValidValue(matcher.group("hour").toLong()).toInt()
            val minute = ChronoField.MINUTE_OF_HOUR.checkValidValue(matcher.group("minute").toLong()).toInt()
            val wholeSecond = ChronoField.SECOND_OF_MINUTE.checkValidValue(matcher.group("second").toLong())
            val fractionPart = matcher.group("fraction")?.let { BigDecimal(".$it") } ?: BigDecimal.ZERO
            val second = BigDecimal.valueOf(wholeSecond).add(fractionPart)
            val timeZoneString = matcher.group("timezone") ?: null
            if (timeZoneString != null) {
                matcher.group("utc")?.let { return Time.of(hour, minute, second, TimeZone.UtcOffset.of(0), null) }
                val timeZone = getTimeZoneComponent(timeZoneString)
                return Time.of(hour, minute, second, timeZone, null)
            }
            return Time.of(hour, minute, second, null, null)
        } catch (e: java.time.DateTimeException) {
            throw DateTimeException("TIME", e.localizedMessage)
        } catch (e: IllegalStateException) {
            throw DateTimeException("TIME", e.localizedMessage)
        } catch (e: IllegalArgumentException) {
            throw DateTimeException("TIME", e.localizedMessage)
        }
    }

    fun parseTimestamp(timestampString: String): Timestamp {
        val matcher: Matcher = TIMESTAMP_PATTERN.matcher(timestampString)
        if (!matcher.matches()) throw DateTimeException(
            "TIMESTAMP",
            "TIMESTAMP Format should be in YYYY-MM-DD, received $timestampString"
        )
        val date = parseDateLiteral(matcher.group("date"))
        val time = parseTimeLiteral(matcher.group("time"))
        return Timestamp.of(date, time)
    }

    private fun getTimeZoneComponent(timezone: String): TimeZone {
        val tzSign = timezone.substring(0, 1).first()
        val tzHour = timezone.substring(1, 3).toInt()
        val tzMinute = timezone.substring(4, 6).toInt()
        return when {
            tzSign == '-' && tzHour == 0 && tzMinute == 0 -> TimeZone.UnknownTimeZone
            tzSign == '-' -> TimeZone.UtcOffset.of(-tzHour, -tzMinute)
            else -> TimeZone.UtcOffset.of(tzHour, tzMinute)
        }
    }
}
