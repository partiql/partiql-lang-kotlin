package org.partiql.lang.ots_work.plugins.standard.functions

import com.amazon.ion.Timestamp
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.dateTimePartValue
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.syntax.DateTimePart

object DateAdd : ScalarFunction {
    override val signature = FunctionSignature(
        name = "date_add",
        requiredParameters = listOf(listOf(SymbolType), listOf(IntType), listOf(TimeStampType)),
        returnType = listOf(TimeStampType)
    )

    private val precisionOrder = listOf(
        Timestamp.Precision.YEAR,
        Timestamp.Precision.MONTH,
        Timestamp.Precision.DAY,
        Timestamp.Precision.MINUTE,
        Timestamp.Precision.SECOND
    )

    private val dateTimePartToPrecision = mapOf(
        DateTimePart.YEAR to Timestamp.Precision.YEAR,
        DateTimePart.MONTH to Timestamp.Precision.MONTH,
        DateTimePart.DAY to Timestamp.Precision.DAY,
        DateTimePart.HOUR to Timestamp.Precision.MINUTE,
        DateTimePart.MINUTE to Timestamp.Precision.MINUTE,
        DateTimePart.SECOND to Timestamp.Precision.SECOND
    )

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

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
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

            return valueFactory.newTimestamp(addedTimestamp)
        } catch (e: IllegalArgumentException) {
            // illegal argument exception are thrown when the resulting timestamp go out of supported timestamp boundaries
            throw EvaluationException(e, errorCode = ErrorCode.EVALUATOR_TIMESTAMP_OUT_OF_BOUNDS, internal = false)
        }
    }
}
