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

// Everything here for CharacterLength() is the same as CharLength(), since they are the same functions.
class CharacterLengthEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(PassCases::class)
    fun characterLengthPassTests(testCase: ExprFunctionTestCase) = assertEval(testCase.source, testCase.expected)

    class PassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("character_length('')", "0"),
            ExprFunctionTestCase("character_length('a')", "1"),
            ExprFunctionTestCase("character_length(`a`)", "1"),
            ExprFunctionTestCase("character_length(`'a'`)", "1"),
            ExprFunctionTestCase("character_length(`\"a\"`)", "1"),
            ExprFunctionTestCase("character_length('ab')", "2"),
            ExprFunctionTestCase("character_length('abcdefghijklmnopqrstuvwxyz')", "26"),
            ExprFunctionTestCase("character_length(null)", "null"),
            ExprFunctionTestCase("character_length(missing)", "null"),
            ExprFunctionTestCase("character_length('ȴȵ💩💋')", "4"),
            ExprFunctionTestCase("character_length('😁😞😸😸')", "4"),
            ExprFunctionTestCase("character_length('話家身圧費谷料村能計税金')", "12"),
            ExprFunctionTestCase("character_length('eࠫ')", "2") // This is a unicode "combining character" which is actually 2 codepoints
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun characterLengthInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "character_length",
        args = listOf(
            Argument(1, StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL), ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun characterLengthInvalidArityTest() = checkInvalidArity(
        funcName = "character_length",
        maxArity = 1,
        minArity = 1
    )
}
