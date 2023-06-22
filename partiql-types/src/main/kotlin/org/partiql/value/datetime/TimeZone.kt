package org.partiql.value.datetime

import kotlin.math.abs

public sealed class TimeZone {
    /**
     * [RFC 3339](https://www.ietf.org/rfc/rfc3339.html): Unknown offset
     */
    public object UnknownTimeZone : TimeZone()

    /**
     * Utc offset is modeled by the difference in **total minutes**
     * between Coordinated Universal Time (UTC) and local time, at a particular place.
     */
    public data class UtcOffset private constructor(val totalOffsetMinutes: Int) : TimeZone() {
        public companion object {

            /**
             * Construct a UtcOffset with both timezone hour and time zone minute.
             * Notice if the time zone is a negative offset, then both [tzHour] and [tzMinute] needs to be negative.
             */
            public fun of(tzHour: Int, tzMinute: Int): UtcOffset {
                if (abs(tzHour) > MAX_TIME_ZONE_HOURS) throw DateTimeException("TimeZone hour field", "should be less than 24")
                if (abs(tzHour) > MAX_TIME_ZONE_MINUTES) throw DateTimeException("Timezone minute fields", "Should be less than 60")
                return UtcOffset(tzHour * 60 + tzMinute)
            }

            public fun of(totalOffsetMinutes: Int): UtcOffset {
                if (abs(totalOffsetMinutes) > MAX_TOTAL_OFFSET_MINUTES) throw DateTimeException("TimeZone hour field", "should be less than 24")
                return UtcOffset(totalOffsetMinutes)
            }
        }
    }

    // TODO: add support for named Time zone (EST, PST, etc)
}
