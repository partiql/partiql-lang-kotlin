package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase

/**
 * More detailed tests are in [DateDiffExprFunctionTest] and parsing related tests in
 * [org.partiql.lang.syntax.SqlParserTest] and [org.partiql.lang.errors.ParserErrorsTest].
 */
class DateDiffEvaluationTest : EvaluatorTestBase() {

    @Test
    fun dateDiffYear() = assertEval("date_diff(year, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "1")

    @Test
    fun dateDiffMonth() = assertEval("date_diff(month, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "12")

    @Test
    fun dateDiffDay() = assertEval("date_diff(day, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "366")

    @Test
    fun dateDiffHour() = assertEval("date_diff(hour, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "8784")

    @Test
    fun dateDiffMinute() = assertEval("date_diff(minute, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "527040")

    @Test
    fun dateDiffSecond() = assertEval("date_diff(second, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "31622400")

    @Test
    fun dateDiffNull01() = assertEval("date_diff(second, null, `2017-01-10T05:30:55Z`)", "null")

    @Test
    fun dateDiffNull02() = assertEval("date_diff(second, `2016-01-10T05:30:55Z`, null)", "null")

    @Test
    fun dateDiffMissing01() = assertEval("date_diff(second, missing, `2017-01-10T05:30:55Z`)", "null")

    @Test
    fun dateDiffMissing02() = assertEval("date_diff(second, `2016-01-10T05:30:55Z`, missing)", "null")

    @Test
    fun dateDiffWithBindings() = assertEval("date_diff(year, a, b)",
                                           "1",
                                           mapOf("a" to "2016-01-10T05:30:55Z",
                                                 "b" to "2017-01-10T05:30:55Z").toSession())

    @Test
    fun wrongArgumentTypes2() =
        checkInputThrowingEvaluationException(
            input = "date_diff(second, 1, `2017-01-10T05:30:55Z`)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "date_diff",
                Property.ARGUMENT_POSITION to 2,
                Property.EXPECTED_ARGUMENT_TYPES to "TIMESTAMP",
                Property.ACTUAL_ARGUMENT_TYPES to "INT",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
            expectedPermissiveModeResult = "MISSING")

    @Test
    fun wrongArgumentTypes3() =
        checkInputThrowingEvaluationException(
            "date_diff(second, `2017-01-10T05:30:55Z`, 1)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "date_diff",
                Property.ARGUMENT_POSITION to 3,
                Property.EXPECTED_ARGUMENT_TYPES to "TIMESTAMP",
                Property.ACTUAL_ARGUMENT_TYPES to "INT",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
            expectedPermissiveModeResult = "MISSING")

}