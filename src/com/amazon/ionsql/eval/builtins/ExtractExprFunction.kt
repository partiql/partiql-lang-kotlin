package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ion.Timestamp.Precision.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*

private const val SECONDS_PER_MINUTE: Int = 60

internal class ExtractExprFunction(ion: IonSystem) : NullPropagatingExprFunction("extract", 2, ion) {
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

        return extracted.exprValue(ion)
    }
}
