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
import org.partiql.lang.eval.time.Time
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

    private fun Timestamp.extractedValue(datePart: DatePart) : Double {
        return when (datePart) {
            DatePart.YEAR -> year
            DatePart.MONTH -> month
            DatePart.DAY -> day
            DatePart.HOUR -> hour
            DatePart.MINUTE -> minute
            DatePart.SECOND -> second
            DatePart.TIMEZONE_HOUR -> hourOffset()
            DatePart.TIMEZONE_MINUTE -> minuteOffset()
        }.toDouble()
    }

    private fun LocalDate.extractedValue(datePart: DatePart) : Double {
        return when (datePart) {
            DatePart.YEAR -> year
            DatePart.MONTH -> monthValue
            DatePart.DAY -> dayOfMonth
            DatePart.TIMEZONE_HOUR,
            DatePart.TIMEZONE_MINUTE -> errNoContext(
                "Timestamp unit ${datePart.name.toLowerCase()} not supported for DATE type",
                internal = false
            )
            DatePart.HOUR, DatePart.MINUTE, DatePart.SECOND -> 0
        }.toDouble()
    }

    private fun Time.extractedValue(datePart: DatePart) : Double {
        return when (datePart) {
            DatePart.HOUR -> localTime.hour.toDouble()
            DatePart.MINUTE -> localTime.minute.toDouble()
            DatePart.SECOND -> secondsWithFractionalPart.toDouble()
            DatePart.TIMEZONE_HOUR -> timezoneHour?.toDouble() ?: errNoContext(
                "Time unit ${datePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
                internal = false
            )
            DatePart.TIMEZONE_MINUTE -> timezoneMinute?.toDouble() ?: errNoContext(
                "Time unit ${datePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
                internal = false
            )
            DatePart.YEAR, DatePart.MONTH, DatePart.DAY -> errNoContext(
                "Time unit ${datePart.name.toLowerCase()} not supported for TIME type.",
                internal = false
            )
        }
    }

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val extractedValue = when(args[1].type) {
            ExprValueType.TIMESTAMP -> args[1].timestampValue().extractedValue(datePart)
            ExprValueType.DATE      -> args[1].dateValue().extractedValue(datePart)
            ExprValueType.TIME      -> args[1].timeValue().extractedValue(datePart)
            else                    -> errNoContext("Expected date or timestamp: ${args[1]}", internal = false)
        }

        return valueFactory.newFloat(extractedValue)
    }

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args[1].isUnknown() -> valueFactory.nullValue
            else                -> eval(env, args)
        }
    }
}
