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

import java.util.regex.Pattern

internal object DateTimeUtil {
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
    internal const val SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR
    internal const val MAX_TIME_ZONE_HOURS: Int = 23
    internal const val MAX_TIME_ZONE_MINUTES: Int = 59
    internal const val MAX_TOTAL_OFFSET_MINUTES: Int = MAX_TIME_ZONE_HOURS * 60 + MAX_TIME_ZONE_MINUTES
}
