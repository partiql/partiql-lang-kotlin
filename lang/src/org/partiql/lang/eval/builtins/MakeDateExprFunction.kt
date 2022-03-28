package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.err
import org.partiql.lang.eval.intValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.propertyValueMapOf
import java.time.DateTimeException

/**
 * Creates a DATE ExprValue from the date fields year, month and day.
 * Takes year, month and day as integers and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_date(<year_value>, <month_value>, <day_value>)
 */
internal class MakeDateExprFunction(val valueFactory: ExprValueFactory) : ExprFunction {

    override val signature = FunctionSignature(
        name = "make_date",
        requiredParameters = listOf(StaticType.INT, StaticType.INT, StaticType.INT),
        returnType = StaticType.DATE
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        required.map {
            if (it.type != ExprValueType.INT) {
                err(
                    message = "Invalid argument type for make_date",
                    errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                    errorContext = propertyValueMapOf(
                        Property.EXPECTED_ARGUMENT_TYPES to "INT",
                        Property.FUNCTION_NAME to "make_date",
                        Property.ACTUAL_ARGUMENT_TYPES to it.type.name
                    ),
                    internal = false
                )
            }
        }

        val (year, month, day) = required.map { it.intValue() }

        try {
            return valueFactory.newDate(year, month, day)
        } catch (e: DateTimeException) {
            err(
                message = "Date field value out of range. $year-$month-$day",
                errorCode = ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
                errorContext = null,
                internal = false
            )
        }
    }
}
