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
import com.amazon.ion.Timestamp.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.partiql.lang.util.*

internal class DateAddExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("date_add", 3, valueFactory) {
    companion object {
        @JvmStatic private val precisionOrder = listOf(Precision.YEAR,
                                                       Precision.MONTH,
                                                       Precision.DAY,
                                                       Precision.MINUTE,
                                                       Precision.SECOND)

        @JvmStatic private val datePartToPrecision = mapOf(DatePart.YEAR to Precision.YEAR,
                                                           DatePart.MONTH to Precision.MONTH,
                                                           DatePart.DAY to Precision.DAY,
                                                           DatePart.HOUR to Precision.MINUTE,
                                                           DatePart.MINUTE to Precision.MINUTE,
                                                           DatePart.SECOND to Precision.SECOND)
    }

    private fun Timestamp.hasSufficientPrecisionFor(requiredPrecision: Precision): Boolean {
        val requiredPrecisionPos = precisionOrder.indexOf(requiredPrecision)
        val precisionPos = precisionOrder.indexOf(precision)

        return precisionPos >= requiredPrecisionPos
    }

    private fun Timestamp.adjustPrecisionTo(datePart: DatePart): Timestamp {
        val requiredPrecision = datePartToPrecision[datePart]!!

        if (this.hasSufficientPrecisionFor(requiredPrecision)) {
            return this
        }

        return when (requiredPrecision) {
            Precision.YEAR     -> Timestamp.forYear(this.year)
            Precision.MONTH    -> Timestamp.forMonth(this.year, this.month)
            Precision.DAY      -> Timestamp.forDay(this.year, this.month, this.day)
            Precision.SECOND   -> Timestamp.forSecond(this.year,
                                                      this.month,
                                                      this.day,
                                                      this.hour,
                                                      this.minute,
                                                      this.second,
                                                      this.localOffset)
            Precision.MINUTE   -> Timestamp.forMinute(this.year,
                                                      this.month,
                                                      this.day,
                                                      this.hour,
                                                      this.minute,
                                                      this.localOffset)
            else                -> errNoContext("invalid date part for date_add: ${datePart.toString().toLowerCase()}",
                                                internal = false)
        }
    }

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val interval = args[1].intValue()
        val timestamp = args[2].timestampValue()

        try {
            val addedTimestamp = when (datePart) {
                DatePart.YEAR   -> timestamp.adjustPrecisionTo(datePart).addYear(interval)
                DatePart.MONTH  -> timestamp.adjustPrecisionTo(datePart).addMonth(interval)
                DatePart.DAY    -> timestamp.adjustPrecisionTo(datePart).addDay(interval)
                DatePart.HOUR   -> timestamp.adjustPrecisionTo(datePart).addHour(interval)
                DatePart.MINUTE -> timestamp.adjustPrecisionTo(datePart).addMinute(interval)
                DatePart.SECOND -> timestamp.adjustPrecisionTo(datePart).addSecond(interval)
                else            -> errNoContext("invalid date part for date_add: ${datePart.toString().toLowerCase()}",
                                                internal = false)
            }

            return valueFactory.newTimestamp(addedTimestamp)
        } catch (e: IllegalArgumentException) {
            // illegal argument exception are thrown when the resulting timestamp go out of supported timestamp boundaries
            throw EvaluationException(e, internal = false)
        }
    }
}


