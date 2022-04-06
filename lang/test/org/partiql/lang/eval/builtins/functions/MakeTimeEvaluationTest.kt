package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
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
    fun runPassTests(testCase: ExprFunctionTestCase) = assertEquals(eval(testCase.source).toString(), testCase.expected)

    class MakeTimePassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("make_time(0, 0, 0.)", "00:00:00"),
            ExprFunctionTestCase("make_time(0, 0, 0., 0)", "00:00:00+00:00"),
            ExprFunctionTestCase("make_time(23, 12, 59.12345)", "23:12:59.12345"),
            ExprFunctionTestCase("make_time(23, 12, 59.12345, 800)", "23:12:59.12345+13:20"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999)", "23:59:59.999999999"),
            ExprFunctionTestCase("make_time(23, 12, 59.12345, -800)", "23:12:59.12345-13:20"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999, -1080)", "23:59:59.999999999-18:00"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999, 1080)", "23:59:59.999999999+18:00"),
            ExprFunctionTestCase("make_time(`23`, `12`, `59.12345`, `800`)", "23:12:59.12345+13:20"),
            ExprFunctionTestCase("make_time(null, 59, 59.999999999)", "NULL"),
            ExprFunctionTestCase("make_time(23, null, 59.999999999)", "NULL"),
            ExprFunctionTestCase("make_time(23, 59, null)", "NULL"),
            ExprFunctionTestCase("make_time(null, 59, 59.999999999, 1080)", "NULL"),
            ExprFunctionTestCase("make_time(23, null, 59.999999999, 1080)", "NULL"),
            ExprFunctionTestCase("make_time(23, 59, null, 1080)", "NULL"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999, null)", "NULL"),
            ExprFunctionTestCase("make_time(missing, 59,59.999999999, 1080)", "NULL"),
            ExprFunctionTestCase("make_time(23, 59, missing, 1080)", "NULL"),
            ExprFunctionTestCase("make_time(23, 59, 59.999999999, missing)", "NULL"),
            ExprFunctionTestCase("make_time(23, 59, missing, null)", "NULL")
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
