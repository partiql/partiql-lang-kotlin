package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.ArgumentsProviderBase

class QueryPowerTest : EvaluatorTestBase() {
    @ParameterizedTest
    @ArgumentsSource(QueryPowerCases::class)
    fun runPassTests(tc: ExprFunctionTestCase) = runEvaluatorTestCase(
        tc.source,
        expectedResult = tc.expectedLegacyModeResult,
        expectedPermissiveModeResult = tc.expectedPermissiveModeResult,
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    class QueryPowerCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("query_power(2)", "4"),
            ExprFunctionTestCase("query_power(2,3)", "8"),
            ExprFunctionTestCase("query_power(2,true)", "32"),
            ExprFunctionTestCase("query_power(null,null)", "null"),
        )
    }

    // Error test cases: Invalid argument type
    data class InvalidTestCase(
        val source: String
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidTestCases::class)
    fun QueryPowerTest(testCase: InvalidTestCase) = runEvaluatorErrorTestCase(
        query = testCase.source,
        expectedErrorCode = ErrorCode.INTERNAL_ERROR,
    )

    class InvalidTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // 1st argument wrong
            InvalidTestCase("query_power(1,1,1)")
        )
    }
}
