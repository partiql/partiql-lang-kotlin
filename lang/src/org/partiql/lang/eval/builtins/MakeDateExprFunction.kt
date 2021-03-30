package org.partiql.lang.eval.builtins

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.*
import java.time.DateTimeException
import java.time.LocalDate

/**
 * Creates a DATE ExprValue from the date fields year, month and day.
 * Takes year, month and day as integers and propagates NULL if any of these arguments is unknown (i.e. NULL or MISSING)
 *
 * make_date(<year_value>, <month_value>, <day_value>)
 */
internal class MakeDateExprFunction(valueFactory: ExprValueFactory) : NullPropagatingExprFunction("make_date", 3, valueFactory) {

    override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
        val errorContext = PropertyValueMap()
        errorContext[Property.EXPECTED_ARGUMENT_TYPES] = "INT"
        errorContext[Property.FUNCTION_NAME] = "make_date"

        args.map {
            if (it.type != ExprValueType.INT) {
                errorContext[Property.ACTUAL_ARGUMENT_TYPES] = it.type.name
                err(
                    message = "Invalid argument type for make_date",
                    errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                    errorContext = null,
                    internal = false
                )
            }
        }

        val (year, month, day) = args.map { it.intValue() }

        try {
            return valueFactory.newDate(LocalDate.of(year, month, day))
        }
        catch (e: DateTimeException) {
            err(
                message = "Date field value out of range. $year-$month-$day",
                errorCode = ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
                errorContext = null,
                internal = false
            )
        }
    }
}