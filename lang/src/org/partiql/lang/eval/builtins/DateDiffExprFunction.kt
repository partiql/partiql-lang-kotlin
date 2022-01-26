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

import com.amazon.ion.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import java.time.*

/**
 * Difference in date parts between two timestamps. If the first timestamp is later than the second the result is negative.
 *
 * Syntax: `DATE_DIFF(<date part>, <timestamp>, <timestamp>)`
 * Where date part is one of the following keywords: `year, month, day, hour, minute, second`
 *
 * Timestamps without all date parts are considered to be in the beginning of the missing parts to make calculation possible.
 * For example:
 * - 2010T is interpreted as 2010-01-01T00:00:00.000Z
 * - date_diff(month, `2010T`, `2010-05T`) results in 4
 *
 * If one of the timestamps has a time component then they are a day apart only if they are 24h apart, examples:
 * - date_diff(day, `2010-01-01T`, `2010-01-02T`) results in 1
 * - date_diff(day, `2010-01-01T23:00Z`, `2010-01-02T01:00Z`) results in 0 as they are only 2h apart
 */
internal class DateDiffExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("date_diff", 3, valueFactory) {

    // Since we don't have a date part for `milliseconds` we can safely set the OffsetDateTime to 0 as it won't
    // affect any of the possible calculations.
    //
    // If we introduce the `milliseconds` date part this will need to be
    // revisited
    private fun Timestamp.toJava() = OffsetDateTime.of(year,
                                                       month,
                                                       day,
                                                       hour,
                                                       minute,
                                                       second,
                                                       0,
                                                       ZoneOffset.ofTotalSeconds((localOffset ?: 0) * 60))

    private fun yearsSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Period.between(left.toLocalDate(), right.toLocalDate()).years

    private fun monthsSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Period.between(left.toLocalDate(), right.toLocalDate()).toTotalMonths()

    private fun daysSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toDays()

    private fun hoursSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toHours()

    private fun minutesSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toMinutes()

    private fun secondsSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toMillis() / 1_000

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val left = args[1].timestampValue()
        val right = args[2].timestampValue()

        val leftAsJava = left.toJava()
        val rightAsJava = right.toJava()

        val difference = when (datePart) {
            DateTimePart.YEAR   -> yearsSince(leftAsJava, rightAsJava)
            DateTimePart.MONTH  -> monthsSince(leftAsJava, rightAsJava)
            DateTimePart.DAY    -> daysSince(leftAsJava, rightAsJava)
            DateTimePart.HOUR   -> hoursSince(leftAsJava, rightAsJava)
            DateTimePart.MINUTE -> minutesSince(leftAsJava, rightAsJava)
            DateTimePart.SECOND -> secondsSince(leftAsJava, rightAsJava)
            else            -> errNoContext("invalid date part for date_diff: ${datePart.toString().toLowerCase()}",
                                            internal = false)
        }

        return valueFactory.newInt(difference.toLong())
    }
}