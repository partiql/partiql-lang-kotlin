package org.partiql.lang.eval.builtins

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.util.ArgumentsProviderBase

class ExistsEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    data class ExistsPassCase(val source: String, val expected: String)

    @ParameterizedTest
    @ArgumentsSource(ExistsPassCases::class)
    fun runPassTests(testCase: ExistsPassCase) = assertEval(testCase.source, testCase.expected)

    class ExistsPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExistsPassCase("EXISTS(<<1, 2, 3>>)", "true"),
            ExistsPassCase("EXISTS(<<>>)", "false"),
            ExistsPassCase("EXISTS(`(1 2 3)`)", "true"),
            ExistsPassCase("EXISTS(`()`)", "false"),
            ExistsPassCase("EXISTS(`[1, 2, 3]`)", "true"),
            ExistsPassCase("EXISTS(`[]`)", "false"),
            ExistsPassCase("EXISTS(`{ a: 1, b: 2, c: 3 }`)", "true"),
            ExistsPassCase("EXISTS(`{}`)", "false")
        )
    }

    // Error test cases
    @Test
    fun existsWithNumberThrowError() =
        checkInputThrowingEvaluationException(
            input = "EXISTS(1)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "exists",
                Property.ARGUMENT_POSITION to 1,
                Property.EXPECTED_ARGUMENT_TYPES to "SEXP, LIST, BAG, or STRUCT",
                Property.ACTUAL_ARGUMENT_TYPES to "INT",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )

    @Test
    fun existsWithSymbolThrowError() =
        checkInputThrowingEvaluationException(
            input = "EXISTS(`a`)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "exists",
                Property.ARGUMENT_POSITION to 1,
                Property.EXPECTED_ARGUMENT_TYPES to "SEXP, LIST, BAG, or STRUCT",
                Property.ACTUAL_ARGUMENT_TYPES to "SYMBOL",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )

    @Test
    fun existsWithStringThrowError() =
        checkInputThrowingEvaluationException(
            input = "EXISTS('a')",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "exists",
                Property.ARGUMENT_POSITION to 1,
                Property.EXPECTED_ARGUMENT_TYPES to "SEXP, LIST, BAG, or STRUCT",
                Property.ACTUAL_ARGUMENT_TYPES to "STRING",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )

    @Test
    fun existsWithTimestampThrowError() =
        checkInputThrowingEvaluationException(
            input = "EXISTS(`2017T`)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "exists",
                Property.ARGUMENT_POSITION to 1,
                Property.EXPECTED_ARGUMENT_TYPES to "SEXP, LIST, BAG, or STRUCT",
                Property.ACTUAL_ARGUMENT_TYPES to "TIMESTAMP",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )

    @Test
    fun existsWithNullThrowError() =
        checkInputThrowingEvaluationException(
            input = "EXISTS(null)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "exists",
                Property.ARGUMENT_POSITION to 1,
                Property.EXPECTED_ARGUMENT_TYPES to "SEXP, LIST, BAG, or STRUCT",
                Property.ACTUAL_ARGUMENT_TYPES to "NULL",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )

    @Test
    fun existsWithMissingThrowError() =
        checkInputThrowingEvaluationException(
            input = "EXISTS(missing)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "exists",
                Property.ARGUMENT_POSITION to 1,
                Property.EXPECTED_ARGUMENT_TYPES to "SEXP, LIST, BAG, or STRUCT",
                Property.ACTUAL_ARGUMENT_TYPES to "MISSING",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )
}
