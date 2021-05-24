package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.*
import org.partiql.lang.eval.time.*
import org.partiql.lang.util.getOffsetHHmm
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.times
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.ZoneOffset

/**
 * Creates a TIME ExprValue from the time fields hour, minute, second and optional timezone_minutes.
 * Takes hour, minute and optional timezone_minutes as integers, second as decimal and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_time(<hour_value>, <minute_value>, <second_value>, <optional_timezone_minutes>?)
 */
internal class MakeTimeExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("make_time", 3..4, valueFactory) {

    private fun ExprValue.validateType(exprValueType: ExprValueType) {
        if (type != exprValueType) {
            err(
                message = "Invalid argument type for make_time",
                errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                errorContext = propertyValueMapOf(
                    Property.EXPECTED_ARGUMENT_TYPES to exprValueType.name,
                    Property.FUNCTION_NAME to "make_time",
                    Property.ACTUAL_ARGUMENT_TYPES to type.name
                ),
                internal = false
            )
        }
    }

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {

        // Validate all the arguments
        val hour: Int = args[0].let {
            it.validateType(ExprValueType.INT)
            it.intValue()
        }

        val minute: Int = args[1].let {
            it.validateType(ExprValueType.INT)
            it.intValue()
        }

        val second: BigDecimal = args[2].let {
            it.validateType(ExprValueType.DECIMAL)
            it.bigDecimalValue()
        }

        val tzMinutes: Int? = when (args.size) {
            4 -> args[3].let {
                it.validateType(ExprValueType.INT)
                it.intValue()
            }
            else -> null
        }

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
        }
        catch (e: EvaluationException) {
            err(
                message = e.message,
                errorCode = ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE,
                errorContext = null,
                internal = false
            )
        }
    }
}