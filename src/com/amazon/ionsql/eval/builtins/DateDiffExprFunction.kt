package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*
import java.time.*

/**
 * Difference in date parts between two timestamps. If the first timestamp is later than the second the result is negative.
 *
 * Syntax: `DATE_DIFF(<date part>, <timestamp>, <timestamp>)`
 * Where date part is one of the following keywords: `year, month, day, hour, minute, second`
 *
 * Timestamps without all date parts are considered to be in the beginning of the missing parts to make calculation possible.
 * For example:
 * - 2010T is interpreted as 2010-01-01T00:00:00.000Z
 * - date_diff(month, `2010T`, `2010-05T`) results in 4
 *
 * If one of the timestamps has a time component then they are a day apart only if they are 24h apart, examples:
 * - date_diff(day, `2010-01-01T`, `2010-01-02T`) results in 1
 * - date_diff(day, `2010-01-01T23:00Z`, `2010-01-02T01:00Z`) results in 0 as they are only 2h apart
 */
internal class DateDiffExprFunction(val ion: IonSystem) : ExprFunction {

    // Since we don't have a date part for `milliseconds` we can safely set the OffsetDateTime to 0 as it won't
    // affect any of the possible calculations.
    //
    // If we introduce the `milliseconds` date part this will need to be
    // revisited
    private fun Timestamp.toJava() = OffsetDateTime.of(year,
                                                       month,
                                                       day,
                                                       hour,
                                                       minute,
                                                       second,
                                                       0,
                                                       ZoneOffset.ofTotalSeconds((localOffset ?: 0) * 60))

    private fun yearsSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Period.between(left.toLocalDate(), right.toLocalDate()).years

    private fun monthsSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Period.between(left.toLocalDate(), right.toLocalDate()).toTotalMonths()

    private fun daysSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toDays()

    private fun hoursSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toHours()

    private fun minutesSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toMinutes()

    private fun secondsSince(left: OffsetDateTime, right: OffsetDateTime): Number =
        Duration.between(left, right).toMillis() / 1_000

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        val (datePart, left, right) = extractArguments(args)

        val leftAsJava = left.toJava()
        val rightAsJava = right.toJava()

        val difference = when (datePart) {
            DatePart.YEAR   -> yearsSince(leftAsJava, rightAsJava)
            DatePart.MONTH  -> monthsSince(leftAsJava, rightAsJava)
            DatePart.DAY    -> daysSince(leftAsJava, rightAsJava)
            DatePart.HOUR   -> hoursSince(leftAsJava, rightAsJava)
            DatePart.MINUTE -> minutesSince(leftAsJava, rightAsJava)
            DatePart.SECOND -> secondsSince(leftAsJava, rightAsJava)
            else            -> errNoContext("invalid date part for date_diff: ${datePart.toString().toLowerCase()}",
                                            internal = false)
        }

        return ion.newInt(difference).exprValue()
    }

    private fun extractArguments(args: List<ExprValue>): Triple<DatePart, Timestamp, Timestamp> {
        return when (args.size) {
            3    -> Triple(args[0].datePartValue(), args[1].timestampValue(), args[2].timestampValue())
            else -> errNoContext("date_diff takes 3 arguments, received: ${args.size}", internal = false)
        }
    }
}