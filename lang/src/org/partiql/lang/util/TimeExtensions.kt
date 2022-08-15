package org.partiql.lang.util

import org.partiql.lang.errors.ErrorCode
import java.time.ZoneOffset
import kotlin.math.absoluteValue

internal const val HOURS_PER_DAY = 24
internal const val MINUTES_PER_HOUR = 60
internal const val SECONDS_PER_MINUTE = 60
internal const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
internal const val NANOS_PER_SECOND = 1000000000
internal const val MAX_PRECISION_FOR_TIME = 9

// These are used to validate the generic format of the time string.
// The more involved logic such as validating the time is done by LocalTime.parse or OffsetTime.parse
internal val timeWithoutTimeZoneRegex = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?")
internal val genericTimeRegex = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?([+|-]\\d\\d:\\d\\d)?")

/**
 * Regex pattern to match date strings of the format yyyy-MM-dd
 */
internal val DATE_PATTERN_REGEX = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")

/**
 * Returns the string representation of the [ZoneOffset] in HH:mm format.
 */
internal fun ZoneOffset.getOffsetHHmm(): String =
    (if (totalSeconds >= 0) "+" else "-") +
        hour.absoluteValue.toString().padStart(2, '0') +
        ":" +
        minute.absoluteValue.toString().padStart(2, '0')

/**
 * Get time zone offset hour
 */
internal val ZoneOffset.hour: Int
    get() = totalSeconds / SECONDS_PER_HOUR

/**
 * Get time zone offset minute
 */
internal val ZoneOffset.minute: Int
    get() = (totalSeconds / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE

/**
 * Get time zone offset in total minutes
 */
internal val ZoneOffset.totalMinutes: Int
    get() = totalSeconds / SECONDS_PER_MINUTE

/**
 * Calculates the precision of a time string based on the fractional component of the 'HH:MM:SS[.ddd....][+|-HH:MM]' format.
 */
internal fun getPrecisionFromTimeString(timeString: String): Int {
    val matcher = genericTimeRegex.toPattern().matcher(timeString)
    if (!matcher.find()) {
        org.partiql.lang.eval.err(
            "Time string does not match the format 'HH:MM:SS[.ddd....][+|-HH:MM]'",
            ErrorCode.PARSE_INVALID_TIME_STRING,
            propertyValueMapOf(),
            false
        )
    }
    // Note that the [genericTimeRegex] has a group to extract the fractional part of the second.
    val fraction = matcher.group(1)?.removePrefix(".")
    return fraction?.length ?: 0
}
