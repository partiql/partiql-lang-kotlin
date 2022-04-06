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

class MakeDateEvaluationTest : EvaluatorTestBase() {
    @ParameterizedTest
    @ArgumentsSource(MakeDatePassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) = assertEquals(eval(testCase.source).toString(), testCase.expected)

    class MakeDatePassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("make_date(100, 1, 1)", "0100-01-01"),
            ExprFunctionTestCase("make_date(1985, 1, 1)", "1985-01-01"),
            ExprFunctionTestCase("make_date(2102, 02, 03)", "2102-02-03"),
            ExprFunctionTestCase("make_date(3000, 02, 03)", "3000-02-03"),
            ExprFunctionTestCase("make_date(2012, 02, 29)", "2012-02-29"),
            ExprFunctionTestCase("make_date(2021, 02, 28)", "2021-02-28"),
            ExprFunctionTestCase("make_date(`100`, `1`, `1`)", "0100-01-01"),
            ExprFunctionTestCase("make_date(NULL, 02, 28)", "NULL"),
            ExprFunctionTestCase("make_date(2021, NULL, 28)", "NULL"),
            ExprFunctionTestCase("make_date(2021, 02, NULL)", "NULL"),
            ExprFunctionTestCase("make_date(MISSING, 02, 28)", "NULL"),
            ExprFunctionTestCase("make_date(MISSING, 02, 28)", "NULL"),
            ExprFunctionTestCase("make_date(2021, MISSING, 28)", "NULL"),
            ExprFunctionTestCase("make_date(2021, 02, MISSING)", "NULL"),
            ExprFunctionTestCase("make_date(NULL, MISSING, 28)", "NULL"),
            ExprFunctionTestCase("make_date(MISSING, NULL, 28)", "NULL"),
            ExprFunctionTestCase("make_date(MISSING, 02, NULL)", "NULL"),
            ExprFunctionTestCase("make_date(NULL, NULL, 28)", "NULL"),
            ExprFunctionTestCase("make_date(NULL, NULL, 28)", "NULL"),
            ExprFunctionTestCase("make_date(MISSING, MISSING, MISSING)", "NULL"),
            ExprFunctionTestCase("make_date(2021, 03, 17) IS DATE", "true")
        )
    }

    // Invalid arguments
    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun makeDateInvalidArgumentTests(query: String) = runEvaluatorErrorTestCase(
        query = query,
        expectedErrorCode = ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
        expectedErrorContext = propertyValueMapOf(1, 1)
    )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            "make_date(2021, 2, 29)",
            "make_date(2021, 4, 31)"
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun makeDateInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "make_date",
        args = listOf(
            Argument(1, StaticType.INT, ","),
            Argument(2, StaticType.INT, ","),
            Argument(3, StaticType.INT, ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun makeDateInvalidArityTest() = checkInvalidArity(
        funcName = "make_date",
        maxArity = 3,
        minArity = 3
    )
}
