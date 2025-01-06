package org.partiql.planner.internal.util

import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.util.regex.Matcher
import java.util.regex.Pattern

internal object DateTimeUtils {

    private val DATE_PATTERN: Pattern = Pattern.compile("(?<year>\\d{4,})-(?<month>\\d{2,})-(?<day>\\d{2,})")
    private val TIME_PATTERN: Pattern = Pattern.compile("(?<hour>\\d{2,}):(?<minute>\\d{2,}):(?<second>\\d{2,})(?:\\.(?<fraction>\\d+))?\\s*(?<timezone>([+-]\\d\\d:\\d\\d)|(?<utc>[Zz]))?")
    private val SQL_TIMESTAMP_DATE_TIME_DELIMITER = "\\s+".toRegex()
    private val RFC8889_TIMESTAMP_DATE_TIME_DELIMITER = "[Tt]".toRegex()
    private val TIMESTAMP_PATTERN = "(?<date>$DATE_PATTERN)($SQL_TIMESTAMP_DATE_TIME_DELIMITER|$RFC8889_TIMESTAMP_DATE_TIME_DELIMITER)(?<time>$TIME_PATTERN)".toRegex().toPattern()

    internal fun parseDate(input: String): LocalDate {
        val matcher: Matcher = DATE_PATTERN.matcher(input)
        if (!matcher.matches()) throw DateTimeException(
            "Expected Date Format to be in YYYY-MM-DD, received $input"
        )
        val year = matcher.group("year").toInt()
        val month = matcher.group("month").toInt()
        val day = matcher.group("day").toInt()
        return LocalDate.of(year, month, day)
    }

    internal fun parseTime(input: String): LocalTime {
        val matcher: Matcher = TIME_PATTERN.matcher(input)
        if (!matcher.matches()) throw DateTimeException(
            "Expect TIME Format to be in HH-mm-ss.ddd+[+|-][hh:mm|z], received $input"
        )
        try {
            val hour = matcher.group("hour").toInt()
            val minute = matcher.group("minute").toInt()
            val second = matcher.group("second").toInt()
            val fraction = matcher.group("fraction")?.let { BigDecimal(".$it") } ?: BigDecimal.ZERO
            val nanoOfSecond = fraction.movePointRight(9).toInt()
            val timeZoneString = matcher.group("timezone") ?: null
            if (timeZoneString != null) {
                throw DateTimeException("Expect TIME Format to be in HH-mm-ss.ddd, received $input")
            }
            return LocalTime.of(hour, minute, second, nanoOfSecond)
        } catch (e: IllegalStateException) {
            throw DateTimeException(e.localizedMessage)
        } catch (e: IllegalArgumentException) {
            throw DateTimeException(e.localizedMessage)
        }
    }

    internal fun parseTimez(input: String): OffsetTime {
        val matcher: Matcher = TIME_PATTERN.matcher(input)
        if (!matcher.matches()) throw DateTimeException(
            "Expect TIME Format to be in HH-mm-ss.ddd+[+|-][hh:mm|z], received $input"
        )
        try {
            val hour = matcher.group("hour").toInt()
            val minute = matcher.group("minute").toInt()
            val second = matcher.group("second").toInt()
            val fraction = matcher.group("fraction")?.let { BigDecimal(".$it") } ?: BigDecimal.ZERO
            val nanoOfSecond = fraction.movePointRight(9).toInt()
            matcher.group("utc")?.let {
                return OffsetTime.of(hour, minute, second, nanoOfSecond, ZoneOffset.UTC)
            }
            // zone
            val timeZoneString = matcher.group("timezone") ?: null
            if (timeZoneString == null) throw DateTimeException("Expect TIME Format to be in HH-mm-ss.ddd+[+|-][hh:mm|z], received $input")
            //
            val timeZone = getTimeZoneComponent(timeZoneString)
            return OffsetTime.of(hour, minute, second, nanoOfSecond, timeZone)
        } catch (e: IllegalStateException) {
            throw DateTimeException(e.localizedMessage)
        } catch (e: IllegalArgumentException) {
            throw DateTimeException(e.localizedMessage)
        }
    }

    fun parseTimestamp(input: String): LocalDateTime {
        val matcher: Matcher = TIMESTAMP_PATTERN.matcher(input)
        if (!matcher.matches()) throw DateTimeException(
            "Expected TIMESTAMP Format should be in YYYY-MM-DD[\\s|T]hh:mm:ss.ddd+[hh:mm|z], received $input"
        )
        val date = parseDate(matcher.group("date"))
        val time = parseTime(matcher.group("time"))
        return LocalDateTime.of(date, time)
    }

    fun parseTimestampz(input: String): OffsetDateTime {
        val matcher: Matcher = TIMESTAMP_PATTERN.matcher(input)
        if (!matcher.matches()) throw DateTimeException(
            "Expected TIMESTAMP Format should be in YYYY-MM-DD[\\s|T]hh:mm:ss.ddd+[hh:mm|z], received $input"
        )
        val date = parseDate(matcher.group("date"))
        val time = parseTimez(matcher.group("time"))
        return OffsetDateTime.of(date, time.toLocalTime(), time.offset)
    }

    private fun getTimeZoneComponent(timezone: String): ZoneOffset {
        val tzSign = timezone.substring(0, 1).first()
        val tzHour = timezone.substring(1, 3).toInt()
        val tzMinute = timezone.substring(4, 6).toInt()
        return when {
            tzSign == '-' && tzHour == 0 && tzMinute == 0 -> error("unknown zone offset")
            tzSign == '-' -> ZoneOffset.ofHoursMinutes(-tzHour, -tzMinute)
            else -> ZoneOffset.ofHoursMinutes(tzHour, tzMinute)
        }
    }
}
