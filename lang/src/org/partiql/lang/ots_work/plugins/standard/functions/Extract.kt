package org.partiql.lang.ots_work.plugins.standard.functions

import com.amazon.ion.Timestamp
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.dateTimePartValue
import org.partiql.lang.eval.dateValue
import org.partiql.lang.eval.errNoContext
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.timeValue
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.DateType
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.TimeStampType
import org.partiql.lang.ots_work.plugins.standard.types.TimeType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.syntax.DateTimePart
import java.math.BigDecimal
import java.time.LocalDate

private const val SECONDS_PER_MINUTE = 60

/**
 * Extracts a date part from a datetime type and returns a [DecimalExprValue] where date part is one of the following keywords:
 * `year, month, day, hour, minute, second, timestamp_hour, timestamp_minute`.
 * Datetime type can be one of DATE, TIME or TIMESTAMP
 * **Note** that the allowed date parts for `EXTRACT` is not the same as `DATE_ADD`
 *
 * Extract does not propagate null for its first parameter, the date part. From the SQL92 spec only the date part
 * keywords are allowed as first argument
 *
 * `EXTRACT(<date part> FROM <datetime_type>)`
 */
object Extract : ScalarFunction {
    override val signature = FunctionSignature(
        name = "extract",
        requiredParameters = listOf(
            listOf(SymbolType),
            listOf(TimeStampType, TimeType(), DateType)
        ),
        returnType = listOf(DecimalType)
    )

    // IonJava Timestamp.localOffset is the offset in minutes, e.g.: `+01:00 = 60` and `-1:20 = -80`
    private fun Timestamp.hourOffset() = (localOffset ?: 0) / SECONDS_PER_MINUTE

    private fun Timestamp.minuteOffset() = (localOffset ?: 0) % SECONDS_PER_MINUTE

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
        return when {
            required[1].isUnknown() -> valueFactory.nullValue
            else -> eval(required)
        }
    }

    private fun Timestamp.extractedValue(dateTimePart: DateTimePart): BigDecimal {
        return when (dateTimePart) {
            DateTimePart.YEAR -> year
            DateTimePart.MONTH -> month
            DateTimePart.DAY -> day
            DateTimePart.HOUR -> hour
            DateTimePart.MINUTE -> minute
            DateTimePart.SECOND -> second
            DateTimePart.TIMEZONE_HOUR -> hourOffset()
            DateTimePart.TIMEZONE_MINUTE -> minuteOffset()
        }.toBigDecimal()
    }

    private fun LocalDate.extractedValue(dateTimePart: DateTimePart): BigDecimal {
        return when (dateTimePart) {
            DateTimePart.YEAR -> year
            DateTimePart.MONTH -> monthValue
            DateTimePart.DAY -> dayOfMonth
            DateTimePart.TIMEZONE_HOUR,
            DateTimePart.TIMEZONE_MINUTE -> errNoContext(
                "Timestamp unit ${dateTimePart.name.toLowerCase()} not supported for DATE type",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
            DateTimePart.HOUR, DateTimePart.MINUTE, DateTimePart.SECOND -> 0
        }.toBigDecimal()
    }

    private fun Time.extractedValue(dateTimePart: DateTimePart): BigDecimal {
        return when (dateTimePart) {
            DateTimePart.HOUR -> localTime.hour.toBigDecimal()
            DateTimePart.MINUTE -> localTime.minute.toBigDecimal()
            DateTimePart.SECOND -> secondsWithFractionalPart
            DateTimePart.TIMEZONE_HOUR -> timezoneHour?.toBigDecimal() ?: errNoContext(
                "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
            DateTimePart.TIMEZONE_MINUTE -> timezoneMinute?.toBigDecimal() ?: errNoContext(
                "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type without TIME ZONE",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
            DateTimePart.YEAR, DateTimePart.MONTH, DateTimePart.DAY -> errNoContext(
                "Time unit ${dateTimePart.name.toLowerCase()} not supported for TIME type.",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }
    }

    private fun eval(args: List<ExprValue>): ExprValue {
        val dateTimePart = args[0].dateTimePartValue()
        val extractedValue = when (args[1].type) {
            ExprValueType.TIMESTAMP -> args[1].timestampValue().extractedValue(dateTimePart)
            ExprValueType.DATE -> args[1].dateValue().extractedValue(dateTimePart)
            ExprValueType.TIME -> args[1].timeValue().extractedValue(dateTimePart)
            else -> errNoContext(
                "Expected date, time or timestamp: ${args[1]}",
                ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
                internal = false
            )
        }

        return valueFactory.newDecimal(extractedValue)
    }
}
