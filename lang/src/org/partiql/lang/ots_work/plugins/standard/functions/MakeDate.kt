package org.partiql.lang.ots_work.plugins.standard.functions

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.err
import org.partiql.lang.eval.intValue
import org.partiql.lang.ots_work.interfaces.function.FunctionSignature
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
import org.partiql.lang.ots_work.plugins.standard.types.DateType
import org.partiql.lang.ots_work.plugins.standard.types.IntType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.util.propertyValueMapOf
import java.time.DateTimeException

/**
 * Creates a DATE ExprValue from the date fields year, month and day.
 * Takes year, month and day as integers and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_date(<year_value>, <month_value>, <day_value>)
 */
object MakeDate : ScalarFunction {
    override val signature = FunctionSignature(
        name = "make_date",
        requiredParameters = listOf(listOf(IntType), listOf(IntType), listOf(IntType)),
        returnType = listOf(DateType)
    )

    override fun callWithRequired(required: List<ExprValue>): ExprValue {
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
