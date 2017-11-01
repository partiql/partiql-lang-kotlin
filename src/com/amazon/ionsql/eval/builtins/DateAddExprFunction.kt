package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*

internal class DateAddExprFunction(val ion: IonSystem) : ExprFunction {
    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        val (datePart, interval, timestamp) = extractArguments(args)

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

    private fun extractArguments(args: List<ExprValue>): Triple<DatePart, Int, Timestamp> {
        return when (args.size) {
            3    -> Triple(args[0].datePartValue(), args[1].intValue(), args[2].timestampValue())
            else -> errNoContext("date_add takes 3 arguments, received: ${args.size}", internal = false)
        }
    }
}