package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.CastTestBase.Companion.runEvaluatorTestCase
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.types.StaticType

class OverlayPositionEvaluationTest : EvaluatorTestBase() {

    @ParameterizedTest
    @ArgumentsSource(OverlayPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) = runEvaluatorTestCase(
        query = testCase.source,
        expectedResult = testCase.expectedLegacyModeResult,
        expectedPermissiveModeResult = testCase.expectedPermissiveModeResult
    )

    class OverlayPassCases : ArgumentsProviderBase() {

        override fun getParameters(): List<Any> = listOf(
            // Special form overlay(<str> PLACING <str> FROM <int> for <int>)
            ExprFunctionTestCase("overlay('hello' placing '' from 1)", "\"hello\""),
            ExprFunctionTestCase("overlay('hello' placing '' from 2 for 3)", "\"ho\""),
            ExprFunctionTestCase("overlay('hello' placing '' from 2 for 4)", "\"h\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 1)", "\"XXllo\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 1 for 3)", "\"XXlo\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 1 for 1)", "\"XXello\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 1 for 100)", "\"XX\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 1 for 0)", "\"XXhello\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 7)", "\"helloXX\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 100 for 100)", "\"helloXX\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 2 for 1)", "\"hXXllo\""),
            ExprFunctionTestCase("overlay('hello' placing 'XX' from 2 for 3)", "\"hXXo\""),
            // Standard syntax overlay(<str>, <str>, <int> [, <int>])
            ExprFunctionTestCase("overlay('hello', '', 1)", "\"hello\""),
            ExprFunctionTestCase("overlay('hello', '', 2, 3)", "\"ho\""),
            ExprFunctionTestCase("overlay('hello', '', 2, 4)", "\"h\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 1)", "\"XXllo\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 1, 3)", "\"XXlo\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 1, 1)", "\"XXello\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 1, 100)", "\"XX\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 1, 0)", "\"XXhello\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 7)", "\"helloXX\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 100, 100)", "\"helloXX\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 2, 1)", "\"hXXllo\""),
            ExprFunctionTestCase("overlay('hello', 'XX', 2, 3)", "\"hXXo\""),
        )
    }

    @Test
    fun positionArgTypeTest() = checkInvalidArgType(
        funcName = "overlay",
        args = listOf(
            Argument(1, StaticType.TEXT, ", "),
            Argument(2, StaticType.TEXT, ", "),
            Argument(3, StaticType.INT, ", "),
            Argument(4, StaticType.INT, ")"),
        )
    )
}
