package org.partiql.lang.datetime

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.TimeWithTimeZone
import org.partiql.value.datetime.TimeWithoutTimeZone
import org.partiql.value.datetime.TimeZone
import org.partiql.value.datetime.Timestamp
import java.math.BigDecimal
import java.util.regex.Matcher
import java.util.regex.Pattern

internal object DateTimeUtils {
    private val DATE_PATTERN: Pattern = Pattern.compile("(?<year>\\d{4,})-(?<month>\\d{2,})-(?<day>\\d{2,})")
    private val TIME_PATTERN: Pattern = Pattern.compile("(?<hour>\\d{2,}):(?<minute>\\d{2,}):(?<decimalSecond>\\d{2,})(?:\\.(?<fraction>\\d+))?\\s*(?<timezone>([+-]\\d\\d:\\d\\d)|(?<utc>[Zz]))?")
    private val SQL_TIMESTAMP_DATE_TIME_DELIMITER = "\\s+".toRegex()
    private val RFC8889_TIMESTAMP_DATE_TIME_DELIMITER = "[Tt]".toRegex()
    private val TIMESTAMP_PATTERN = "(?<date>$DATE_PATTERN)($SQL_TIMESTAMP_DATE_TIME_DELIMITER|$RFC8889_TIMESTAMP_DATE_TIME_DELIMITER)(?<time>$TIME_PATTERN)".toRegex().toPattern()

    internal fun parseDateLiteral(dateString: String): Date {
        val matcher: Matcher = DATE_PATTERN.matcher(dateString)
        if (!matcher.matches()) throw DateTimeException(
            "Expected Date Format to be in YYYY-MM-DD, received $dateString"
        )
        val year = matcher.group("year").toInt()
        val month = matcher.group("month").toInt()
        val day = matcher.group("day").toInt()
        return DateTimeValue.date(year, month, day)
    }

    internal fun parseTimeLiteral(timeString: String): Time {
        val matcher: Matcher = TIME_PATTERN.matcher(timeString)
        if (!matcher.matches()) throw DateTimeException(
            "Expect TIME Format to be in HH-mm-ss.ddd+[+|-][hh:mm|z], received $timeString"
        )
        try {
            val hour = matcher.group("hour").toInt()
            val minute = matcher.group("minute").toInt()
            val wholeSecond = matcher.group("decimalSecond").toLong()
            val fractionPart = matcher.group("fraction")?.let { BigDecimal(".$it") } ?: BigDecimal.ZERO
            val second = BigDecimal.valueOf(wholeSecond).add(fractionPart)
            val timeZoneString = matcher.group("timezone") ?: null
            if (timeZoneString != null) {
                matcher.group("utc")?.let { return DateTimeValue.time(hour, minute, second, TimeZone.UtcOffset.of(0)) }
                val timeZone = getTimeZoneComponent(timeZoneString)
                return DateTimeValue.time(hour, minute, second, timeZone)
            }
            return DateTimeValue.time(hour, minute, second)
        } catch (e: IllegalStateException) {
            throw DateTimeException(e.localizedMessage)
        } catch (e: IllegalArgumentException) {
            throw DateTimeException(e.localizedMessage)
        }
    }

    internal fun parseTimestamp(timestampString: String): Timestamp {
        val matcher: Matcher = TIMESTAMP_PATTERN.matcher(timestampString)
        if (!matcher.matches()) throw DateTimeException(
            "Expected TIMESTAMP Format should be in YYYY-MM-DD[\\s|T]hh:mm:ss.ddd+[hh:mm|z], received $timestampString"
        )
        val date = parseDateLiteral(matcher.group("date"))
        val time = parseTimeLiteral(matcher.group("time"))
        return when (time) {
            is TimeWithTimeZone -> DateTimeValue.timestamp(date, time)
            is TimeWithoutTimeZone -> DateTimeValue.timestamp(date, time)
        }
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
