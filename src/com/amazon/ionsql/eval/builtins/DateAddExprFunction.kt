package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ion.Timestamp.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*

internal class DateAddExprFunction(ion: IonSystem) : NullPropagatingExprFunction("date_add", 3, ion) {
    companion object {
        @JvmStatic private val precisionOrder = listOf(Precision.YEAR,
                                                       Precision.MONTH,
                                                       Precision.DAY,
                                                       Precision.MINUTE,
                                                       Precision.SECOND,
                                                       Precision.FRACTION)

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
            // should be unreachable
            Precision.FRACTION -> errNoContext("Requiring FRACTION precision when highest allowed is SECOND",
                                               internal = true)
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

            return ion.newTimestamp(addedTimestamp).exprValue()
        } catch (e: IllegalArgumentException) {
            // illegal argument exception are thrown when the resulting timestamp go out of supported timestamp boundaries
            throw EvaluationException(e, internal = false)
        }
    }
}


