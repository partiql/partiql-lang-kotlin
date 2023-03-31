package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.checkInvalidArity
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.types.StaticType

// Everything here for CharacterLength() is the same as CharLength(), since they are the same functions.
class CharacterLengthEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(PassCases::class)
    fun characterLengthPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

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
            ExprFunctionTestCase("character_length(missing)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("character_length('ȴȵ💩💋')", "4"),
            ExprFunctionTestCase("character_length('😁😞😸😸')", "4"),
            ExprFunctionTestCase("character_length('話家身圧費谷料村能計税金')", "12"),
            ExprFunctionTestCase("character_length('eࠫ')", "2"), // This is a unicode "combining character" which is actually 2 codepoints
            // Same thing, different name. We shouldn't have to duplicate tests just for an alternative name
            ExprFunctionTestCase("char_length('')", "0"),
            ExprFunctionTestCase("char_length('a')", "1"),
            ExprFunctionTestCase("char_length(`a`)", "1"),
            ExprFunctionTestCase("char_length(`'a'`)", "1"),
            ExprFunctionTestCase("char_length(`\"a\"`)", "1"),
            ExprFunctionTestCase("char_length('ab')", "2"),
            ExprFunctionTestCase("char_length('abcdefghijklmnopqrstuvwxyz')", "26"),
            ExprFunctionTestCase("char_length(null)", "null"),
            ExprFunctionTestCase("char_length(missing)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("char_length('ȴȵ💩💋')", "4"),
            ExprFunctionTestCase("char_length('😁😞😸😸')", "4"),
            ExprFunctionTestCase("char_length('話家身圧費谷料村能計税金')", "12"),
            ExprFunctionTestCase("char_length('eࠫ')", "2"), // This is a unicode "combining character" which is actually 2 codepoints
            // Just a few tests for a smoke check, since we have already tested the same function above.
            ExprFunctionTestCase("length('')", "0"),
            ExprFunctionTestCase("length('hello')", "5"),
            ExprFunctionTestCase("length('eࠫ')", "2"), // This is a unicode "combining character" which is actually 2 codepoints
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun characterLengthInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "character_length",
        args = listOf(
            Argument(1, StaticType.TEXT, ")")
        )
    )

    // Error test cases: Invalid arity
    @Test
    fun characterLengthInvalidArityTest() = checkInvalidArity(
        funcName = "character_length",
        maxArity = 1,
        minArity = 1
    )

    @Test
    fun charLengthInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "char_length",
        args = listOf(
            Argument(1, StaticType.TEXT, ")")
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
