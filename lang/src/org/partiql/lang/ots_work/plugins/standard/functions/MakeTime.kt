package org.partiql.lang.ots_work.plugins.standard.functions

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.bigDecimalValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.Time
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.DecimalType
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.types.TimeType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import java.math.BigDecimal

/**
 * Creates a TIME ExprValue from the time fields hour, minute, second and optional timezone_minutes.
 * Takes hour, minute and optional timezone_minutes as integers, second as decimal and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_time(<hour_value>, <minute_value>, <second_value>, <optional_timezone_minutes>?)
 */
object MakeTime : ScalarFunction {
    override val signature = FunctionSignature(
        name = "make_time",
        requiredParameters = listOf(listOf(IntType), listOf(IntType), listOf(DecimalType)),
        optionalParameter = listOf(IntType),
        returnType = listOf(TimeType())
    )

    override fun callWithOptional(required: List<ExprValue>, optional: ExprValue): ExprValue {
        val (hour, min, sec) = required
        return makeTime(hour.intValue(), min.intValue(), sec.bigDecimalValue(), optional.intValue())
    }

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
        val (hour, min, sec) = required
        return makeTime(hour.intValue(), min.intValue(), sec.bigDecimalValue(), null)
    }

    private fun makeTime(
        hour: Int,
        minute: Int,
        second: BigDecimal,
        tzMinutes: Int?
    ): ExprValue {
        try {
            return valueFactory.newTime(
                Time.of(
                    hour,
                    minute,
                    second.toInt(),
                    (second.remainder(BigDecimal.ONE).multiply(NANOS_PER_SECOND.toBigDecimal())).toInt(),
                    second.scale(),
                    tzMinutes
                )
            )
        } catch (e: EvaluationException) {
            err(
                message = e.message,
                errorCode = ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE,
                errorContext = null,
                internal = false
            )
        }
    }
}
