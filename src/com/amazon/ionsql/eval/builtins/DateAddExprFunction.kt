package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*

internal class DateAddExprFunction(val ion: IonSystem) : ExprFunction {
    private fun ExprValue.timePartValue() = DatePart.valueOf(this.stringValue().toUpperCase())

    private fun ExprValue.intValue(): Int = this.numberValue().toInt()

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        val (timePart, interval, timestamp) = extractArguments(args)

        val addedTimestamp = when (timePart) {
            DatePart.YEAR        -> timestamp.addYear(interval)
            DatePart.MONTH       -> timestamp.addMonth(interval)
            DatePart.DAY         -> timestamp.addDay(interval)
            DatePart.HOUR        -> timestamp.addHour(interval)
            DatePart.MINUTE      -> timestamp.addMinute(interval)
            DatePart.SECOND      -> timestamp.addSecond(interval)
        }

        return ion.newTimestamp(addedTimestamp).exprValue()
    }

    private fun extractArguments(args: List<ExprValue>): Triple<DatePart, Int, Timestamp> {
        return when (args.size) {
            3    -> Triple(args[0].timePartValue(), args[1].intValue(), args[2].timestampValue())
            else -> errNoContext("date_add takes 3 arguments, received: ${args.size}", internal = false)
        }
    }
}