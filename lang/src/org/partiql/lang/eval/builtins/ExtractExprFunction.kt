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
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.dateTimePartValue
import org.partiql.lang.eval.dateValue
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.timeValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.syntax.DateTimePart
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import java.math.BigDecimal
import java.time.LocalDate

private const val SECONDS_PER_MINUTE = 60

/**
 * Extracts a date part from a datetime type and returns a [DecimalExprValue] where date part is one of the following keywords:
 * `year, month, day, hour, minute, second, timestamp_hour, timestamp_minute`.
 * Datetime type can be one of DATE, TIME or TIMESTAMP
 * **Note** that the allowed date parts for `EXTRACT` is not the same as `DATE_ADD`
 *
 * Extract does not propagate null for its first parameter, the date part. From the SQL92 spec only the date part
 * keywords are allowed as first argument
 *
 * `EXTRACT(<date part> FROM <datetime_type>)`
 */
internal class ExtractExprFunction(val valueFactory: ExprValueFactory) : ExprFunction {
    override val signature = FunctionSignature(
        name = "extract",
        requiredParameters = listOf(
            StaticType.SYMBOL,
            AnyOfType(
                setOf(
                    StaticType.TIMESTAMP,
                    StaticType.TIME,
                    StaticType.DATE
                )
            )
        ),
        returnType = StaticType.DECIMAL
    )

    // IonJava Timestamp.localOffset is the offset in minutes, e.g.: `+01:00 = 60` and `-1:20 = -80`
    private fun Timestamp.hourOffset() = (localOffset ?: 0) / SECONDS_PER_MINUTE

    private fun Timestamp.minuteOffset() = (localOffset ?: 0) % SECONDS_PER_MINUTE

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        return when {
            required[1].isUnknown() -> valueFactory.nullValue
            else -> eval(required)
        }
    }

    private fun Timestamp.extractedValue(dateTimePart: DateTimePart): BigDecimal {
        return when (dateTimePart) {
            DateTimePart.YEAR -> year
            DateTimePart.MONTH -> month
            DateTimePart.DAY -> day
            DateTimePart.HOUR -> hour
            DateTimePart.MINUTE -> minute
            DateTimePart.SECOND -> second
            DateTimePart.TIMEZONE_HOUR -> hourOffset()
            DateTimePart.TIMEZONE_MINUTE -> minuteOffset()
        }.toBigDecimal()
    }

    private fun LocalDate.extractedValue(dateTimePart: DateTimePart): BigDecimal {
        return when (dateTimePart) {
            DateTimePart.YEAR -> year
            DateTimePart.MONTH -> monthValue
            DateTimePart.DAY -> dayOfMonth
            DateTimePart.TIMEZONE_HOUR,
            DateTimePart.TIMEZONE_MINUTE -> errNoContext(
                "Timestamp unit ${dateTimePart.name.toLowerCase()} not supported for DATE type",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
            DateTimePart.HOUR, DateTimePart.MINUTE, DateTimePart.SECOND -> 0
        }.toBigDecimal()
    }

    private fun Time.extractedValue(dateTimePart: DateTimePart): BigDecimal {
        return when (dateTimePart) {
            DateTimePart.HOUR -> localTime.hour.toBigDecimal()
            DateTimePart.MINUTE -> localTime.minute.toBigDecimal()
            DateTimePart.SECOND -> secondsWithFractionalPart
            DateTimePart.TIMEZONE_HOUR -> timezoneHour?.toBigDecimal() ?: errNoContext(
                "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
            DateTimePart.TIMEZONE_MINUTE -> timezoneMinute?.toBigDecimal() ?: errNoContext(
                "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
            DateTimePart.YEAR, DateTimePart.MONTH, DateTimePart.DAY -> errNoContext(
                "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type.",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }
    }

    private fun eval(args: List<ExprValue>): ExprValue {
        val dateTimePart = args[0].dateTimePartValue()
        val extractedValue = when (args[1].type) {
            ExprValueType.TIMESTAMP -> args[1].timestampValue().extractedValue(dateTimePart)
            ExprValueType.DATE -> args[1].dateValue().extractedValue(dateTimePart)
            ExprValueType.TIME -> args[1].timeValue().extractedValue(dateTimePart)
            else -> errNoContext(
                "Expected date, time or timestamp: ${args[1]}",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }

        return valueFactory.newDecimal(extractedValue)
    }
}
