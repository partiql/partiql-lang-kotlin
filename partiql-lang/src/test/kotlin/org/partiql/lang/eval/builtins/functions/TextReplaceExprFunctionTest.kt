package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.util.ArgumentsProviderBase

class TextReplaceExprFunctionTest : EvaluatorTestBase() {
    @ParameterizedTest
    @ArgumentsSource(TestCases::class)
    fun runTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    class TestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("text_replace('abcdefabcdef', 'cd', 'XX')", "\"abXXefabXXef\""),
            ExprFunctionTestCase("text_replace('abcdefabcdef', 'xyz', 'XX')", "\"abcdefabcdef\""),
            ExprFunctionTestCase("text_replace('abcdefabcdef', 'defab', '')", "\"abccdef\""),
            ExprFunctionTestCase("text_replace('abcabcabcdef', 'abcabc', 'XXX')", "\"XXXabcdef\""),
            ExprFunctionTestCase("text_replace('abcabcabcdef', '', 'X')", "\"XaXbXcXaXbXcXaXbXcXdXeXfX\""),
            ExprFunctionTestCase("text_replace('', 'abc', 'XX')", "\"\""),
            ExprFunctionTestCase("text_replace('', '', 'XX')", "\"XX\""),
            ExprFunctionTestCase("text_replace('abcdefabcdef', 'def', '😁😞')", "\"abc😁😞abc😁😞\""),
            ExprFunctionTestCase("text_replace('abc😁😞abc😁😞', '😁😞', 'def')", "\"abcdefabcdef\""),
            ExprFunctionTestCase("text_replace('abcdefabcdef', 'def', 'd😁😞')", "\"abcd😁😞abcd😁😞\""),
            ExprFunctionTestCase("text_replace('abcdefabcdef', 'def', 'deࠫf')", "\"abcdeࠫfabcdeࠫf\""),
            ExprFunctionTestCase("text_replace('abcdeࠫfabcdeࠫf', 'def', 'XX')", "\"abcdeࠫfabcdeࠫf\""),
            ExprFunctionTestCase("text_replace('abcdeࠫfabcdeࠫf', 'deࠫf', 'XX')", "\"abcXXabcXX\""),
        )
    }
}
