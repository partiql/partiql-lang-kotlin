/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.builtins

import com.amazon.ion.Timestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.TemporalAdjusters


internal fun Timestamp.toTemporalAccessor() = TimestampTemporalAccessor(this)

internal fun Timestamp.toOffsetDateTime(): OffsetDateTime =
        java.time.OffsetDateTime.of(
                this.year,
                this.month,
                this.day,
                this.hour,
                this.minute,
                this.second,
                this.decimalSecond.rem(java.math.BigDecimal.valueOf(1L)).multiply(java.math.BigDecimal.valueOf(1000000000)).toInt(),
                java.time.ZoneOffset.ofTotalSeconds(this.localOffset * 60))

internal fun OffsetDateTime.toTimestamp(): com.amazon.ion.Timestamp =
        Timestamp.forSecond(this.year, this.month.value, this.dayOfMonth, this.hour, this.minute,
                BigDecimal.valueOf(this.second.toLong()).plus(
                        BigDecimal.valueOf(this.nano.toLong()).divide(BigDecimal.valueOf(1000000000))),
                this.offset.totalSeconds / 60)

internal fun java.util.Random.nextTimestamp(): Timestamp {
    val year = Math.abs(this.nextInt() % 9999) + 1
    val month = Math.abs(this.nextInt() % 12) + 1

    //Determine last day of month for randomly generated month & year (e.g. 28, 29, 30 or 31)
    val maxDayOfMonth = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth

    val day = Math.abs(this.nextInt() % maxDayOfMonth) + 1
    val hour = Math.abs(this.nextInt() % 24)
    val minute = Math.abs(this.nextInt() % 60)

    val secondFraction = BigDecimal.valueOf(Math.abs(this.nextLong()) % 1000000000).div(BigDecimal.valueOf(1000000000L))
    val seconds = BigDecimal.valueOf(Math.abs(this.nextInt() % 59L)).add(secondFraction).abs()
    //Note:  need to % 59L above because 59L + secondFraction can yield 60 seconds

    var offsetMinutes = this.nextInt() % (18 * 60)

    //If the offset pushes this timestamp before 1/1/0001 then we will get IllegalArgumentException from
    //Timestamp.forSecond
    //NOTE:  the offset is *substracted* from the specified time!
    if(year == 1 && month == 1 && day == 1 && hour <= 18) {
        offsetMinutes = -Math.abs(offsetMinutes)
    }
    //Same if the offset can push this time stamp after 12/31/9999
    else if (year == 9999 && month == 12 && day == 31 && hour >= 6) {
        offsetMinutes = Math.abs(offsetMinutes)
    }
    return Timestamp.forSecond(year, month, day, hour, minute, seconds, offsetMinutes)
}
