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

internal class DateAddExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("date_add", 3, valueFactory) {
    companion object {
        @JvmStatic private val precisionOrder = listOf(Precision.YEAR,
                                                       Precision.MONTH,
                                                       Precision.DAY,
                                                       Precision.MINUTE,
                                                       Precision.SECOND)

        @JvmStatic private val dateTimePartToPrecision = mapOf(DateTimePart.YEAR to Precision.YEAR,
                                                           DateTimePart.MONTH to Precision.MONTH,
                                                           DateTimePart.DAY to Precision.DAY,
                                                           DateTimePart.HOUR to Precision.MINUTE,
                                                           DateTimePart.MINUTE to Precision.MINUTE,
                                                           DateTimePart.SECOND to Precision.SECOND)
    }

    private fun Timestamp.hasSufficientPrecisionFor(requiredPrecision: Precision): Boolean {
        val requiredPrecisionPos = precisionOrder.indexOf(requiredPrecision)
        val precisionPos = precisionOrder.indexOf(precision)

        return precisionPos >= requiredPrecisionPos
    }

    private fun Timestamp.adjustPrecisionTo(dateTimePart: DateTimePart): Timestamp {
        val requiredPrecision = dateTimePartToPrecision[dateTimePart]!!

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
            else                -> errNoContext("invalid date part for date_add: ${dateTimePart.toString().toLowerCase()}",
                                                internal = false)
        }
    }

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val interval = args[1].intValue()
        val timestamp = args[2].timestampValue()

        try {
            val addedTimestamp = when (datePart) {
                DateTimePart.YEAR   -> timestamp.adjustPrecisionTo(datePart).addYear(interval)
                DateTimePart.MONTH  -> timestamp.adjustPrecisionTo(datePart).addMonth(interval)
                DateTimePart.DAY    -> timestamp.adjustPrecisionTo(datePart).addDay(interval)
                DateTimePart.HOUR   -> timestamp.adjustPrecisionTo(datePart).addHour(interval)
                DateTimePart.MINUTE -> timestamp.adjustPrecisionTo(datePart).addMinute(interval)
                DateTimePart.SECOND -> timestamp.adjustPrecisionTo(datePart).addSecond(interval)
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


