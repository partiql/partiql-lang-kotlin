package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*

internal class DateAddExprFunction(ion: IonSystem) : NullPropagatingExprFunction("date_add", 3, ion) {
    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val interval = args[1].intValue()
        val timestamp = args[2].timestampValue()

        try {
            val addedTimestamp = when (datePart) {
                DatePart.YEAR   -> timestamp.addYear(interval)
                DatePart.MONTH  -> timestamp.addMonth(interval)
                DatePart.DAY    -> timestamp.addDay(interval)
                DatePart.HOUR   -> timestamp.addHour(interval)
                DatePart.MINUTE -> timestamp.addMinute(interval)
                DatePart.SECOND -> timestamp.addSecond(interval)
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