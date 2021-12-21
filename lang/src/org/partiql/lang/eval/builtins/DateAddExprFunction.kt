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
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.NullPropagatingExprFunction
import org.partiql.lang.eval.datePartValue
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.syntax.DatePart

internal class DateAddExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("date_add", 3, valueFactory) {
    companion object {
        @JvmStatic private val precisionOrder = listOf(Timestamp.Precision.YEAR,
                Timestamp.Precision.MONTH,
                Timestamp.Precision.DAY,
                Timestamp.Precision.MINUTE,
                Timestamp.Precision.SECOND)

        @JvmStatic private val datePartToPrecision = mapOf(DatePart.YEAR to Timestamp.Precision.YEAR,
                                                           DatePart.MONTH to Timestamp.Precision.MONTH,
                                                           DatePart.DAY to Timestamp.Precision.DAY,
                                                           DatePart.HOUR to Timestamp.Precision.MINUTE,
                                                           DatePart.MINUTE to Timestamp.Precision.MINUTE,
                                                           DatePart.SECOND to Timestamp.Precision.SECOND)
    }

    private fun Timestamp.hasSufficientPrecisionFor(requiredPrecision: Timestamp.Precision): Boolean {
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
            Timestamp.Precision.YEAR     -> Timestamp.forYear(this.year)
            Timestamp.Precision.MONTH    -> Timestamp.forMonth(this.year, this.month)
            Timestamp.Precision.DAY      -> Timestamp.forDay(this.year, this.month, this.day)
            Timestamp.Precision.SECOND   -> Timestamp.forSecond(this.year,
                                                      this.month,
                                                      this.day,
                                                      this.hour,
                                                      this.minute,
                                                      this.second,
                                                      this.localOffset)
            Timestamp.Precision.MINUTE   -> Timestamp.forMinute(this.year,
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


