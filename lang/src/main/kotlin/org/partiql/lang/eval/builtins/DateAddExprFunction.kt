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
import com.amazon.ion.Timestamp.Precision
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.dateTimePartValue
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.exprTimestamp
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.syntax.DateTimePart
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType

internal class DateAddExprFunction : ExprFunction {
    override val signature = FunctionSignature(
        name = "date_add",
        requiredParameters = listOf(StaticType.SYMBOL, StaticType.INT, StaticType.TIMESTAMP),
        returnType = StaticType.TIMESTAMP
    )

    companion object {
        @JvmStatic private val precisionOrder = listOf(
            Timestamp.Precision.YEAR,
            Timestamp.Precision.MONTH,
            Timestamp.Precision.DAY,
            Timestamp.Precision.MINUTE,
            Timestamp.Precision.SECOND
        )
        @JvmStatic private val dateTimePartToPrecision = mapOf(
            DateTimePart.YEAR to Precision.YEAR,
            DateTimePart.MONTH to Precision.MONTH,
            DateTimePart.DAY to Precision.DAY,
            DateTimePart.HOUR to Precision.MINUTE,
            DateTimePart.MINUTE to Precision.MINUTE,
            DateTimePart.SECOND to Precision.SECOND
        )
    }

    private fun Timestamp.hasSufficientPrecisionFor(requiredPrecision: Timestamp.Precision): Boolean {
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
            Timestamp.Precision.YEAR -> Timestamp.forYear(this.year)
            Timestamp.Precision.MONTH -> Timestamp.forMonth(this.year, this.month)
            Timestamp.Precision.DAY -> Timestamp.forDay(this.year, this.month, this.day)
            Timestamp.Precision.SECOND -> Timestamp.forSecond(
                this.year,
                this.month,
                this.day,
                this.hour,
                this.minute,
                this.second,
                this.localOffset
            )
            Timestamp.Precision.MINUTE -> Timestamp.forMinute(
                this.year,
                this.month,
                this.day,
                this.hour,
                this.minute,
                this.localOffset
            )
            else -> errNoContext(
                "invalid datetime part for date_add: ${dateTimePart.toString().toLowerCase()}",
                errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART,
                internal = false
            )
        }
    }

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val dateTimePart = required[0].dateTimePartValue()
        val interval = required[1].intValue()
        val timestamp = required[2].timestampValue()

        try {
            val addedTimestamp = when (dateTimePart) {
                DateTimePart.YEAR -> timestamp.adjustPrecisionTo(dateTimePart).addYear(interval)
                DateTimePart.MONTH -> timestamp.adjustPrecisionTo(dateTimePart).addMonth(interval)
                DateTimePart.DAY -> timestamp.adjustPrecisionTo(dateTimePart).addDay(interval)
                DateTimePart.HOUR -> timestamp.adjustPrecisionTo(dateTimePart).addHour(interval)
                DateTimePart.MINUTE -> timestamp.adjustPrecisionTo(dateTimePart).addMinute(interval)
                DateTimePart.SECOND -> timestamp.adjustPrecisionTo(dateTimePart).addSecond(interval)
                else -> errNoContext(
                    "invalid datetime part for date_add: ${dateTimePart.toString().toLowerCase()}",
                    errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_DATE_PART,
                    internal = false
                )
            }

            return exprTimestamp(addedTimestamp)
        } catch (e: IllegalArgumentException) {
            // illegal argument exception are thrown when the resulting timestamp go out of supported timestamp boundaries
            throw EvaluationException(e, errorCode = ErrorCode.EVALUATOR_TIMESTAMP_OUT_OF_BOUNDS, internal = false)
        }
    }
}
