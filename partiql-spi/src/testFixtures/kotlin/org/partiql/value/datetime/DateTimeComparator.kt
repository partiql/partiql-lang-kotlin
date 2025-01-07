/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.value.datetime

internal object DateTimeComparator {

    fun compareTo(left: Date, right: Date): Int {
        var cmp = left.year.compareTo(right.year)
        if (cmp == 0) {
            cmp = left.month.compareTo(right.month)
            if (cmp == 0) {
                cmp = left.day.compareTo(right.day)
            }
        }
        return cmp
    }

    fun compareTo(left: TimeWithoutTimeZone, right: TimeWithoutTimeZone): Int {
        var cmp = left.hour.compareTo(right.hour)
        if (cmp == 0) {
            cmp = left.minute.compareTo(right.minute)
            if (cmp == 0) {
                cmp = left.decimalSecond.compareTo(right.decimalSecond)
            }
        }
        return cmp
    }

    fun compareTo(left: TimeWithTimeZone, right: TimeWithTimeZone): Int {
        val leftUtc = left.toTimeWithoutTimeZone(TimeZone.UtcOffset.of(0))
        val rightUtc = right.toTimeWithoutTimeZone(TimeZone.UtcOffset.of(0))
        return compareTo(leftUtc, rightUtc)
    }

    fun compareTo(left: TimestampWithoutTimeZone, right: TimestampWithoutTimeZone): Int {
        var cmp = compareTo(left.toDate(), right.toDate())
        if (cmp == 0) {
            cmp = compareTo(left.toTime(), right.toTime())
        }
        return cmp
    }

    fun compareTo(left: TimestampWithTimeZone, right: TimestampWithTimeZone): Int {
        var cmp = compareTo(left.toDate(), right.toDate())
        if (cmp == 0) {
            cmp = compareTo(left.toTime(), right.toTime())
        }
        return cmp
    }

    fun compareTo(left: Time, right: Time) =
        when (left) {
            is TimeWithTimeZone -> {
                when (right) {
                    is TimeWithTimeZone -> compareTo(left, right)
                    is TimeWithoutTimeZone ->
                        throw DateTimeException("Can not compare Time With Time Zone Value and Time Without Time Zone value")
                }
            }
            is TimeWithoutTimeZone -> {
                when (right) {
                    is TimeWithTimeZone ->
                        throw DateTimeException("Can not compare Time With Time Zone Value and Time Without Time Zone value")
                    is TimeWithoutTimeZone -> compareTo(left, right)
                }
            }
        }

    fun compareTo(left: Timestamp, right: Timestamp) =
        when (left) {
            is TimestampWithTimeZone -> {
                when (right) {
                    is TimestampWithTimeZone -> compareTo(left, right)
                    is TimestampWithoutTimeZone ->
                        throw DateTimeException("Can not compare Timestamp With Time Zone Value and Timestamp Without Time Zone value")
                }
            }
            is TimestampWithoutTimeZone -> {
                when (right) {
                    is TimestampWithTimeZone ->
                        throw DateTimeException("Can not compare Timestamp With Time Zone Value and Timestamp Without Time Zone value")
                    is TimestampWithoutTimeZone -> compareTo(left, right)
                }
            }
        }
}
