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

import java.time.LocalDate
import kotlin.jvm.Throws

/**
 * Date represents a calendar system, (i.e., 2023-06-01).
 * It does not include information on time or timezone, instead, it is meant to represent a specific date on calendar.
 * For example, 2022-11-25 (black friday in 2022).
 * The valid range are from 0001-01-01 to 9999-12-31
 * The [day] must be valid for the year and month, otherwise an exception will be thrown.
 */
public data class SqlDate private constructor(
    val localDate: LocalDate
) : Date {
    public companion object {
        /**
         * Construct a Date object using
         */
        @JvmStatic
        @Throws(DateTimeException::class)
        public fun of(year: Int, month: Int, day: Int): Date {
            if (year < 1 || year > 9999)
                throw DateTimeException("Expect Year Field to be between 1 to 9999, but received $year")
            try {
                return SqlDate(LocalDate.of(year, month, day))
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage, e)
            }
        }
    }

    public override val year: Int = localDate.year

    public override val month: Int = localDate.monthValue

    public override val day: Int = localDate.dayOfMonth

    public val epochDays: Long by lazy {
        this.localDate.toEpochDay()
    }

    override fun atTime(time: Time): Timestamp =
        when(time) {
            is TimeWithoutTimeZone -> LocalTimestampHighPrecision.forDateTime(this, time)
            is TimeWithTimeZone -> OffsetTimestampHighPrecision.forDateTime(this, time)
        }

    // Operation
    public override fun plusDays(days: Long): Date =
        this.localDate.plusDays(days)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
    public override fun plusMonths(months: Long): Date =
        this.localDate.plusMonths(months)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
    public override fun plusYear(years: Long): Date =
        this.localDate.plusYears(years)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }


}
