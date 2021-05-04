package org.partiql.lang.util

import org.partiql.lang.eval.time.SECONDS_PER_HOUR
import org.partiql.lang.eval.time.SECONDS_PER_MINUTE
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.absoluteValue


// These are used to validate the generic format of the time string.
// The more involved logic such as validating the time is done by LocalTime.parse or OffsetTime.parse
internal val timeWithoutTimeZoneRegex = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?")
internal val genericTimeRegex = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?([+|-]\\d\\d:\\d\\d)?")

/**
 * Regex pattern to match date strings of the format yyyy-MM-dd
 */
internal val DATE_PATTERN_REGEX = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")

internal val LOCAL_TIMEZONE_OFFSET = ZoneOffset.systemDefault().rules.getOffset(Instant.now())

/**
 * Returns the string representation of the [ZoneOffset] in HH:mm format.
 */
internal fun ZoneOffset.getOffsetHHmm(): String =
    (if(totalSeconds >= 0) "+" else "-") +
    hour.absoluteValue.toString().padStart(2, '0') +
    ":" +
    minute.absoluteValue.toString().padStart(2, '0')

/**
 * Get time zone offset hour
 */
internal val ZoneOffset.hour : Int
    get() = totalSeconds / SECONDS_PER_HOUR

/**
 * Get time zone offset minute
 */
internal val ZoneOffset.minute : Int
    get() = (totalSeconds / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE

/**
 * Gets the precision from the time string of the format 'HH:MM:SS[.ddd....][+|-HH:MM]'.
 */
internal fun getPrecisionFromTimeString(timeString: String) : Int {
    val matcher = genericTimeRegex.toPattern().matcher(timeString)
    if (!matcher.find()) {
        org.partiql.lang.eval.err("Time string does not match the format 'HH:MM:SS[.ddd....][+|-HH:MM]'",
            propertyValueMapOf(),
            false
        )
    }
    // Note that the [genericTimeRegex] has a group to extract the fractional part of the second.
    val fraction = matcher.group(1)?.removePrefix(".")
    return fraction?.length ?: 0
}
