package org.partiql.value.datetime

import java.util.regex.Pattern

internal val DATETIME_PATTERN = Pattern.compile(
    "(?<year>[-+]?\\d{4,})-(?<month>\\d{1,2})-(?<day>\\d{1,2})" +
        "(?: (?<hour>\\d{1,2}):(?<minute>\\d{1,2})(?::(?<second>\\d{1,2})(?:\\.(?<fraction>\\d+))?)?)?" +
        "\\s*(?<timezone>[+-]\\d\\d:\\d\\d)?"
)

internal val DATE_PATTERN = Pattern.compile("(?<year>\\d{4,})-(?<month>\\d{2,})-(?<day>\\d{2,})")

internal val TIME_PATTERN =
    Pattern.compile("(?<hour>\\d{2,}):(?<minute>\\d{2,}):(?<second>\\d{2,})(?:\\.(?<fraction>\\d+))?\\s*(?<timezone>[+-]\\d\\d:\\d\\d)?")

internal const val MILLIS_IN_SECOND: Long = 1000
internal const val MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND
internal const val MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE
internal const val MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR
internal const val SECONDS_IN_MINUTE = 60L
internal const val SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE
internal const val MAX_TIME_ZONE_HOURS: Int = 23
internal const val MAX_TIME_ZONE_MINUTES: Int = 59
internal const val MAX_TOTAL_OFFSET_MINUTES: Int = MAX_TIME_ZONE_HOURS * 60 + MAX_TIME_ZONE_MINUTES

public class DateTimeException(
    public val type: String,
    public val error: String
) : Throwable()
