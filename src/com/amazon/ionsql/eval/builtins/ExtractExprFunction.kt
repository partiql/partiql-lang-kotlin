package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ion.Timestamp.Precision.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*

internal class ExtractExprFunction(ion: IonSystem) : NullPropagatingExprFunction("extract", 2, ion) {
    private val SECONDS_PER_MINUTE = 60

    // IonJava Timestamp.localOffset is the offset in minutes, e.g.: `+01:00 = 60` and `-1:20 = -80`
    private fun Timestamp.hourOffset() = (localOffset ?: 0) / SECONDS_PER_MINUTE
    private fun Timestamp.minuteOffset() = (localOffset ?: 0) % SECONDS_PER_MINUTE

    private val precisionOrder = listOf(YEAR, MONTH, DAY, MINUTE, SECOND)
    private val minimumPrecisionRequired = mapOf(
        DatePart.YEAR to YEAR,
        DatePart.MONTH to MONTH,
        DatePart.DAY to DAY,
        DatePart.HOUR to MINUTE,
        DatePart.MINUTE to MINUTE,
        DatePart.SECOND to SECOND,
        DatePart.TIMEZONE_HOUR to MINUTE,
        DatePart.TIMEZONE_MINUTE to MINUTE)

    private fun Timestamp.hasInsufficientPrecisionFor(datePart: DatePart): Boolean {
        val datePartPos = precisionOrder.indexOf(minimumPrecisionRequired[datePart])
        val precisionPos = precisionOrder.indexOf(precision)

        return precisionPos < datePartPos
    }

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val datePart = args[0].datePartValue()
        val timestamp = args[1].timestampValue()

        if(timestamp.hasInsufficientPrecisionFor(datePart)){
            return nullExprValue(ion)
        }

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

        return extracted.exprValue(ion)
    }
}


