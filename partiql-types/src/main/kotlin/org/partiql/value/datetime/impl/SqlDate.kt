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

package org.partiql.value.datetime.impl

import org.partiql.value.datetime.Date
import org.partiql.value.datetime.DateImpl
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeValue.timestamp
import org.partiql.value.datetime.Time
import org.partiql.value.datetime.Timestamp
import java.time.LocalDate
import kotlin.jvm.Throws

internal data class SqlDate private constructor(
    val localDate: LocalDate
) : DateImpl() {
    companion object {
        /**
         * Construct a Date object using
         */
        @JvmStatic
        @Throws(DateTimeException::class)
        fun of(year: Int, month: Int, day: Int): Date {
            if (year < 1 || year > 9999)
                throw DateTimeException("Expect Year Field to be between 1 to 9999, but received $year")
            try {
                return SqlDate(LocalDate.of(year, month, day))
            } catch (e: java.time.DateTimeException) {
                throw DateTimeException(e.localizedMessage, e)
            }
        }
    }

    override val year: Int = localDate.year

    override val month: Int = localDate.monthValue

    override val day: Int = localDate.dayOfMonth

    override fun atTime(time: Time): Timestamp = timestamp(this, time)

    // Operation
    override fun plusDays(days: Long): Date =
        this.localDate.plusDays(days)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
    override fun plusMonths(months: Long): Date =
        this.localDate.plusMonths(months)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
    override fun plusYear(years: Long): Date =
        this.localDate.plusYears(years)
            .let { newDate ->
                of(newDate.year, newDate.monthValue, newDate.dayOfMonth)
            }
}
