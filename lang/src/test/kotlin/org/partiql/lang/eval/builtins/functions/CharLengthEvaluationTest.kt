package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

class CharLengthEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(PassCases::class)
    fun charLengthPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    class PassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("char_length('')", "0"),
            ExprFunctionTestCase("char_length('a')", "1"),
            ExprFunctionTestCase("char_length(`a`)", "1"),
            ExprFunctionTestCase("char_length(`'a'`)", "1"),
            ExprFunctionTestCase("char_length(`\"a\"`)", "1"),
            ExprFunctionTestCase("char_length('ab')", "2"),
            ExprFunctionTestCase("char_length('abcdefghijklmnopqrstuvwxyz')", "26"),
            ExprFunctionTestCase("char_length(null)", "NULL"),
            ExprFunctionTestCase("char_length(missing)", "NULL", "MISSING"),
            ExprFunctionTestCase("char_length('ȴȵ💩💋')", "4"),
            ExprFunctionTestCase("char_length('😁😞😸😸')", "4"),
            ExprFunctionTestCase("char_length('話家身圧費谷料村能計税金')", "12"),
            ExprFunctionTestCase("char_length('eࠫ')", "2"), // This is a unicode "combining character" which is actually 2 codepoints
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun charLengthInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "char_length",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL), ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun charLengthInvalidArityTest() = checkInvalidArity(
        funcName = "char_length",
        maxArity = 1,
        minArity = 1
    )
}
