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
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.dateTimePartValue
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.syntax.DateTimePart
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import java.time.Duration
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneOffset

/**
 * Difference in datetime parts between two timestamps. If the first timestamp is later than the second the result is negative.
 *
 * Syntax: `DATE_DIFF(<datetime part>, <timestamp>, <timestamp>)`
 * Where date time part is one of the following keywords: `year, month, day, hour, minute, second`
 *
 * Timestamps without all datetime parts are considered to be in the beginning of the missing parts to make calculation possible.
 * For example:
 * - 2010T is interpreted as 2010-01-01T00:00:00.000Z
 * - date_diff(month, `2010T`, `2010-05T`) results in 4
 *
 * If one of the timestamps has a time component then they are a day apart only if they are 24h apart, examples:
 * - date_diff(day, `2010-01-01T`, `2010-01-02T`) results in 1
 * - date_diff(day, `2010-01-01T23:00Z`, `2010-01-02T01:00Z`) results in 0 as they are only 2h apart
 */
internal class DateDiffExprFunction(val valueFactory: ExprValueFactory) : ExprFunction {
    override val signature = FunctionSignature(
        name = "date_diff",
        requiredParameters = listOf(StaticType.SYMBOL, StaticType.TIMESTAMP, StaticType.TIMESTAMP),
        returnType = StaticType.INT
    )

    // Since we don't have a datetime part for `milliseconds` we can safely set the OffsetDateTime to 0 as it won't
    // affect any of the possible calculations.
    //
    // If we introduce the `milliseconds` datetime part this will need to be
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

    override fun callWithRequired(env: Environment, required: List<ExprValue>): ExprValue {
        val dateTimePart = required[0].dateTimePartValue()
        val left = required[1].timestampValue()
        val right = required[2].timestampValue()

        val leftAsJava = left.toJava()
        val rightAsJava = right.toJava()

        val difference = when (dateTimePart) {
            DateTimePart.YEAR   -> yearsSince(leftAsJava, rightAsJava)
            DateTimePart.MONTH  -> monthsSince(leftAsJava, rightAsJava)
            DateTimePart.DAY    -> daysSince(leftAsJava, rightAsJava)
            DateTimePart.HOUR   -> hoursSince(leftAsJava, rightAsJava)
            DateTimePart.MINUTE -> minutesSince(leftAsJava, rightAsJava)
            DateTimePart.SECOND -> secondsSince(leftAsJava, rightAsJava)
            else            -> errNoContext("invalid datetime part for date_diff: ${dateTimePart.toString().toLowerCase()}",
                                            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART,
                                            internal = false)
        }

        return valueFactory.newInt(difference.toLong())
    }
}