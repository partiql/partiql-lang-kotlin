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

private const val SECONDS_PER_MINUTE = 60

/**
 * Extracts a date part from a timestamp where date part is one of the following keywords:
 * `year, month, day, hour, minute, second, timestamp_hour, timestamp_minute`.
 *
 * **Note** that the allowed date parts for `EXTRACT` is not the same as `DATE_ADD`
 *
 * Extract does not propagate null for its first parameter, the date part. From the SQL92 spec only the date part
 * keywords are allowed as first argument
 *
 * `EXTRACT(<date part> FROM <timestamp>)`
 */
internal class ExtractExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("extract", 2, valueFactory) {

    // IonJava Timestamp.localOffset is the offset in minutes, e.g.: `+01:00 = 60` and `-1:20 = -80`
    private fun Timestamp.hourOffset() = (localOffset ?: 0) / SECONDS_PER_MINUTE

    private fun Timestamp.minuteOffset() = (localOffset ?: 0) % SECONDS_PER_MINUTE

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val timestamp = args[1].timestampValue()

        val extracted = when (datePart) {
            DatePart.YEAR            -> timestamp.year
            DatePart.MONTH           -> timestamp.month
            DatePart.DAY             -> timestamp.day
            DatePart.HOUR            -> timestamp.hour
            DatePart.MINUTE          -> timestamp.minute
            DatePart.SECOND          -> timestamp.second
            DatePart.TIMEZONE_HOUR   -> timestamp.hourOffset()
            DatePart.TIMEZONE_MINUTE -> timestamp.minuteOffset()
        }

        return valueFactory.newInt(extracted)
    }

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args[1].isUnknown() -> valueFactory.nullValue
            else                -> eval(env, args)
        }
    }
}
