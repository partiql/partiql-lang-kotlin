package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.ExpectedResultFormat
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf

class MakeTimeEvaluationTest : EvaluatorTestBase() {
    @ParameterizedTest
    @ArgumentsSource(MakeTimePassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(
            query = testCase.source,
            expectedLegacyModeResult = testCase.expectedLegacyModeResult,
            expectedPermissiveModeResult = testCase.expectedPermissiveModeResult,
            expectedResultFormat = ExpectedResultFormat.ION
        )

    class MakeTimePassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("make_time(0, 0, 0.)", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
            ExprFunctionTestCase("make_time(0, 0, 0., 0)", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:0,timezone_minute:0}"),
            ExprFunctionTestCase("make_time(23, 12, 59.12345)", "\$partiql_time::{hour:23,minute:12,second:59.12345,timezone_hour:null.int,timezone_minute:null.int}"),
            ExprFunctionTestCase("make_time(23, 12, 59.12345, 800)", "\$partiql_time::{hour:23,minute:12,second:59.12345,timezone_hour:13,timezone_minute:20}"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999)", "\$partiql_time::{hour:23,minute:59,second:59.999999999,timezone_hour:null.int,timezone_minute:null.int}"),
            ExprFunctionTestCase("make_time(23, 12, 59.12345, -800)", "\$partiql_time::{hour:23,minute:12,second:59.12345,timezone_hour:-13,timezone_minute:-20}"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999, -1080)", "\$partiql_time::{hour:23,minute:59,second:59.999999999,timezone_hour:-18,timezone_minute:0}"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999, 1080)", "\$partiql_time::{hour:23,minute:59,second:59.999999999,timezone_hour:18,timezone_minute:0}"),
        )
    }

    // Error test cases: Invalid arguments
    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun makeTimeInvalidArgumentTests(query: String) = runEvaluatorErrorTestCase(
        query = query,
        expectedErrorCode = ErrorCode.EVALUATOR_TIME_FIELD_OUT_OF_RANGE,
        expectedErrorContext = propertyValueMapOf(1, 1)
    )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            "make_time(24, 0, 0.)",
            "make_time(23, 60, 0.)",
            "make_time(23, 59, 60.)",
            "make_time(23, 59, 59.999999999, -1081)",
            "make_time(23, 59, 59.999999999, 1081)"
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun makeTimeInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "make_time",
        args = listOf(
            Argument(1, StaticType.INT, ","),
            Argument(2, StaticType.INT, ","),
            Argument(3, StaticType.DECIMAL, ","),
            Argument(4, StaticType.INT, ")"),
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun makeTimeInvalidArityTest() = checkInvalidArity(
        funcName = "make_time",
        minArity = 3,
        maxArity = 4
    )
}
