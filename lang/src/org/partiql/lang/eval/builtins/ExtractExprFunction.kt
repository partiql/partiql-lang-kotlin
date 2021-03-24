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
import java.time.LocalDate

private const val SECONDS_PER_MINUTE = 60

/**
 * Extracts a date part from a datetime type where date part is one of the following keywords:
 * `year, month, day, hour, minute, second, timestamp_hour, timestamp_minute`.
 * Datetime type can be one of DATE, TIME or TIMESTAMP
 * **Note** that the allowed date parts for `EXTRACT` is not the same as `DATE_ADD`
 *
 * Extract does not propagate null for its first parameter, the date part. From the SQL92 spec only the date part
 * keywords are allowed as first argument
 *
 * `EXTRACT(<date part> FROM <datetime_type>)`
 */
internal class ExtractExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("extract", 2, valueFactory) {

    // IonJava Timestamp.localOffset is the offset in minutes, e.g.: `+01:00 = 60` and `-1:20 = -80`
    private fun Timestamp.hourOffset() = (localOffset ?: 0) / SECONDS_PER_MINUTE

    private fun Timestamp.minuteOffset() = (localOffset ?: 0) % SECONDS_PER_MINUTE

    private fun extractedValue(datePart: DatePart, dateTimeValue: Any?) : Int {
        return when (dateTimeValue) {
            is Timestamp -> when (datePart) {
                DatePart.YEAR -> dateTimeValue.year
                DatePart.MONTH -> dateTimeValue.month
                DatePart.DAY -> dateTimeValue.day
                DatePart.HOUR -> dateTimeValue.hour
                DatePart.MINUTE -> dateTimeValue.minute
                DatePart.SECOND -> dateTimeValue.second
                DatePart.TIMEZONE_HOUR -> dateTimeValue.hourOffset()
                DatePart.TIMEZONE_MINUTE -> dateTimeValue.minuteOffset()
            }
            is LocalDate -> when (datePart) {
                DatePart.YEAR -> dateTimeValue.year
                DatePart.MONTH -> dateTimeValue.monthValue
                DatePart.DAY -> dateTimeValue.dayOfMonth
                DatePart.TIMEZONE_HOUR,
                DatePart.TIMEZONE_MINUTE -> errNoContext("Timestamp unit ${datePart.name.toLowerCase()} not supported for DATE type", internal = false)
                else -> 0
            }
            else         -> errNoContext("Expected date or timestamp: $dateTimeValue", internal = false)
        }
    }
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val dateTimeValue = when(args[1].type) {
            ExprValueType.TIMESTAMP -> args[1].timestampValue()
            ExprValueType.DATE      -> args[1].dateValue()

            else                    -> errNoContext("Expected date or timestamp: ${args[1]}", internal = false)
        }

        return valueFactory.newInt(extractedValue(datePart, dateTimeValue))
    }

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args[1].isUnknown() -> valueFactory.nullValue
            else                -> eval(env, args)
        }
    }
}
