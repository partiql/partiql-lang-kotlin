/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.value.datetime

import org.partiql.value.datetime.DateTimeUtil.MAX_TIME_ZONE_HOURS
import org.partiql.value.datetime.DateTimeUtil.MAX_TIME_ZONE_MINUTES
import org.partiql.value.datetime.DateTimeUtil.MAX_TOTAL_OFFSET_MINUTES
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
        val tzHour: Int by lazy {
            this.totalOffsetMinutes / 60
        }

        val tzMinute: Int by lazy {
            this.totalOffsetMinutes - this.tzHour * 60
        }
        public companion object {

            /**
             * Construct a UtcOffset with both timezone hour and time zone minute.
             * Notice if the time zone is a negative offset, then both [tzHour] and [tzMinute] needs to be negative.
             */
            @JvmStatic
            @Throws(DateTimeException::class)
            public fun of(tzHour: Int, tzMinute: Int): UtcOffset {
                if (abs(tzHour) > MAX_TIME_ZONE_HOURS) throw DateTimeException("Except Timezone Hour to be less than 24, but received $tzHour")
                if (abs(tzMinute) > MAX_TIME_ZONE_MINUTES) throw DateTimeException("Except Timezone Minute to be less than 60, but received $tzMinute")
                return UtcOffset(tzHour * 60 + tzMinute)
            }

            @JvmStatic
            @Throws(DateTimeException::class)
            public fun of(totalOffsetMinutes: Int): UtcOffset {
                if (abs(totalOffsetMinutes) > MAX_TOTAL_OFFSET_MINUTES) throw DateTimeException("Expect total offset Minutes to be less than or equal to $MAX_TOTAL_OFFSET_MINUTES, but received $totalOffsetMinutes")
                return UtcOffset(totalOffsetMinutes)
            }
        }
    }

    // TODO: add support for named Time zone (EST, PST, etc)
}
