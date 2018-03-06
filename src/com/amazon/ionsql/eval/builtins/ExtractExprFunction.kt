package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*

private const val SECONDS_PER_MINUTE = 60

/**
 * Extracts a date part from a timestamp where date part is one of the following keywords:
 * `year, month, day, hour, minute, second, timestamp_hour, timestamp_minute`.
 *
 * **Note** that the allowed date parts for `EXTRACT` is not the same as `DATE_ADD`
 *
 * Extract does not propagate null for it's first parameter, the date part. From the SQL92 spec only the date part
 * keywords are allowed as first argument
 *
 * `EXTRACT(<date part> FROM <timestamp>)`
 */
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

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args[1].type == ExprValueType.NULL    -> nullExprValue(ion)
            args[1].type == ExprValueType.MISSING -> missingExprValue(ion)
            else                                  -> eval(env, args)
        }
    }
}
