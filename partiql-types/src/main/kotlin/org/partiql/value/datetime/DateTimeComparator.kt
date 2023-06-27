package org.partiql.value.datetime

/**
 * This is its own class, because in the future we may want to migrate the time and date in the base,
 * and be able to compare/case between those types. (having all three extend from Datetime interface)
 */
// TODO: Consider model Date, Time, Timestamp to have a common parent.
internal object DateTimeComparator {

    internal fun compareTimestamp(left: Timestamp, right: Timestamp): Int {
        return when {
            left.timeZone != null && right.timeZone != null -> left.epochSecond.compareTo(right.epochSecond)
            // for timestamp without time zones, assume UTC and compare
            left.timeZone == null && right.timeZone == null -> {
                left.copy(timeZone = TimeZone.UtcOffset.of(0))
                    .compareTo(right.copy(timeZone = TimeZone.UtcOffset.of(0)))
            }

            else -> throw DateTimeComparisonException(
                left,
                right,
                "Can not compare between timestamp with time zone and timestamp without time zone",
            )
        }
    }
}
