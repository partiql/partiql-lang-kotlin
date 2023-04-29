package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.types.StaticType

class PositionEvaluationTest : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(PositionPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) = runEvaluatorTestCase(
        query = testCase.source,
        expectedResult = testCase.expectedLegacyModeResult,
        expectedPermissiveModeResult = testCase.expectedPermissiveModeResult
    )

    class PositionPassCases : ArgumentsProviderBase() {

        override fun getParameters(): List<Any> = listOf(
            // Special form position(<str> IN <str>)
            ExprFunctionTestCase("position('foo' in 'hello')", "0"),
            ExprFunctionTestCase("position('' in 'hello')", "1"),
            ExprFunctionTestCase("position('h' in 'hello')", "1"),
            ExprFunctionTestCase("position('o' in 'hello')", "5"),
            ExprFunctionTestCase("position('ll' in 'hello')", "3"),
            ExprFunctionTestCase("position('lo' in 'hello')", "4"),
            ExprFunctionTestCase("position('hello' in 'hello')", "1"),
            ExprFunctionTestCase("position('xx' in 'xxyyxxyy')", "1"),
            ExprFunctionTestCase("position('yy' in 'xxyyxxyy')", "3"),
            // position(<str>, <str>)
            ExprFunctionTestCase("position('foo', 'hello')", "0"),
            ExprFunctionTestCase("position('', 'hello')", "1"),
            ExprFunctionTestCase("position('h', 'hello')", "1"),
            ExprFunctionTestCase("position('o', 'hello')", "5"),
            ExprFunctionTestCase("position('ll', 'hello')", "3"),
            ExprFunctionTestCase("position('lo', 'hello')", "4"),
            ExprFunctionTestCase("position('hello', 'hello')", "1"),
            ExprFunctionTestCase("position('xx', 'xxyyxxyy')", "1"),
            ExprFunctionTestCase("position('yy', 'xxyyxxyy')", "3"),
        )
    }

    @Test
    fun positionArgTypeTest() = checkInvalidArgType(
        funcName = "position",
        args = listOf(
            Argument(1, StaticType.TEXT, ","),
            Argument(2, StaticType.TEXT, ")"),
        )
    )
}
