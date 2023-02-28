package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.bigDecimalValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.intValue
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.Time
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import java.math.BigDecimal

/**
 * Creates a TIME ExprValue from the time fields hour, minute, second and optional timezone_minutes.
 * Takes hour, minute and optional timezone_minutes as integers, second as decimal and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_time(<hour_value>, <minute_value>, <second_value>, <optional_timezone_minutes>?)
 */
internal class MakeTimeExprFunction : ExprFunction {
    override val signature = FunctionSignature(
        name = "make_time",
        requiredParameters = listOf(StaticType.INT, StaticType.INT, StaticType.DECIMAL),
        optionalParameter = StaticType.INT,
        returnType = StaticType.TIME
    )

    override fun callWithOptional(session: EvaluationSession, required: List<ExprValue>, opt: ExprValue): ExprValue {
        val (hour, min, sec) = required
        return makeTime(hour.intValue(), min.intValue(), sec.bigDecimalValue(), opt.intValue())
    }

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
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
            return ExprValue.newTime(
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
                errorContext = e.errorContext,
                internal = false
            )
        }
    }
}
