package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

class SubstringEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(SubstringPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, expectedResult = testCase.expectedLegacyModeResult)

    class SubstringPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // Old syntax: substring(<str> FROM <start pos> [FOR <length>])
            ExprFunctionTestCase("substring('abcdefghi' FROM 0)", "\"abcdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi' FROM 1)", "\"abcdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi' FROM -1)", "\"abcdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi' FROM 3)", "\"cdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi' FROM 3 FOR 20)", "\"cdefghi\""),
            ExprFunctionTestCase("substring('1234567890' FROM 10)", "\"0\""),
            ExprFunctionTestCase("substring('1234567890' FROM 11)", "\"\""),
            ExprFunctionTestCase("substring('1234567890' FROM 10 FOR 10)", "\"0\""),
            ExprFunctionTestCase("substring('1234567890' FROM 11 FOR 10)", "\"\""),
            ExprFunctionTestCase("substring('abcdefghi' FROM 3 FOR 4)", "\"cdef\""),
            ExprFunctionTestCase("substring('abcdefghi' FROM -1 FOR 4)", "\"ab\""),
            ExprFunctionTestCase("substring('abcdefghi' FROM 1 FOR 1)", "\"a\""),
            ExprFunctionTestCase("substring('😁😞😸😸' FROM 2 FOR 2)", "\"😞😸\""),
            ExprFunctionTestCase("substring('話家身圧費谷料村能計税金' FROM 3 FOR 5)", "\"身圧費谷料\""),
            ExprFunctionTestCase("substring('話家身圧費谷料村能計税金' FROM -3 FOR 6)", "\"話家\""),
            ExprFunctionTestCase(
                "substring('abcde\u0832fgh' FROM 3 FOR 6)",
                "\"cde\u0832fg\""
            ), // Note:  U+0832 is a "combining diacritical mark" https://en.wikipedia.org/wiki/Combining_character.
            // Even though it is visually merged with the preceding letter when displayed, it still counts as a distinct codepoint.
            ExprFunctionTestCase("substring(null FROM 1)", "null"),
            ExprFunctionTestCase("substring('abc' FROM null)", "null"),
            ExprFunctionTestCase("substring(null FROM 1 FOR 1)", "null"),
            ExprFunctionTestCase("substring('abc' FROM null FOR 1)", "null"),
            ExprFunctionTestCase("substring('abc' FROM 1 FOR null)", "null"),
            ExprFunctionTestCase("substring(missing FROM 1)", "null"),
            ExprFunctionTestCase("substring('abc' FROM missing)", "null"),
            ExprFunctionTestCase("substring(missing FROM 1 FOR 1)", "null"),
            ExprFunctionTestCase("substring('abc' FROM missing FOR 1)", "null"),
            ExprFunctionTestCase("substring('' FROM -1)", "\"\""),
            ExprFunctionTestCase("substring('' FROM 0)", "\"\""),
            ExprFunctionTestCase("substring('' FROM 99)", "\"\""),
            ExprFunctionTestCase("substring('' FROM -1 FOR 999)", "\"\""),
            ExprFunctionTestCase("substring('' FROM 0 FOR 999)", "\"\""),
            ExprFunctionTestCase("substring('' FROM -4 FOR 1)", "\"\""),
            ExprFunctionTestCase("substring('1' FROM -4 FOR 1)", "\"\""),
            // New syntax: substring(<str>, <start pos> [, <length>])
            ExprFunctionTestCase("substring('abcdefghi', 0)", "\"abcdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi', 1)", "\"abcdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi', 1)", "\"abcdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi', -1)", "\"abcdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi', 3)", "\"cdefghi\""),
            ExprFunctionTestCase("substring('abcdefghi', 3, 20)", "\"cdefghi\""),
            ExprFunctionTestCase("substring('1234567890', 10)", "\"0\""),
            ExprFunctionTestCase("substring('1234567890', 11)", "\"\""),
            ExprFunctionTestCase("substring('1234567890', 10, 10)", "\"0\""),
            ExprFunctionTestCase("substring('1234567890', 11, 10)", "\"\""),
            ExprFunctionTestCase("substring('abcdefghi', 3, 4)", "\"cdef\""),
            ExprFunctionTestCase("substring('abcdefghi', -1, 4)", "\"ab\""),
            ExprFunctionTestCase("substring('abcdefghi', 1, 1)", "\"a\""),
            ExprFunctionTestCase("substring('😁😞😸😸', 2, 2)", "\"😞😸\""),
            ExprFunctionTestCase("substring('話家身圧費谷料村能計税金', 3, 5)", "\"身圧費谷料\""),
            ExprFunctionTestCase("substring('話家身圧費谷料村能計税金', -3, 6)", "\"話家\""),
            ExprFunctionTestCase("substring('abcde\u0832fgh', 3, 6)", "\"cde\u0832fg\""),
            ExprFunctionTestCase("substring(null, 1)", "null"),
            ExprFunctionTestCase("substring('abc', null)", "null"),
            ExprFunctionTestCase("substring(null, 1, 1)", "null"),
            ExprFunctionTestCase("substring('abc', null, 1)", "null"),
            ExprFunctionTestCase("substring('abc', 1, null)", "null"),
            ExprFunctionTestCase("substring(missing, 1)", "null"),
            ExprFunctionTestCase("substring('abc', missing)", "null"),
            ExprFunctionTestCase("substring(missing, 1, 1)", "null"),
            ExprFunctionTestCase("substring('abc', missing, 1)", "null"),
            ExprFunctionTestCase("substring('abc', 1, missing)", "null"),
            ExprFunctionTestCase("substring('', -1)", "\"\""),
            ExprFunctionTestCase("substring('', 0)", "\"\""),
            ExprFunctionTestCase("substring('', 99)", "\"\""),
            ExprFunctionTestCase("substring('', -1, 999)", "\"\""),
            ExprFunctionTestCase("substring('', 0, 999)", "\"\""),
            ExprFunctionTestCase("substring('', -4, 1)", "\"\""),
            ExprFunctionTestCase("substring('1', -4, 1)", "\"\"")
        )
    }

    // Error test cases: Invalid argument type
    // Old syntax
    @Test
    fun substringInvalidArgTypeTest1() = checkInvalidArgType(
        funcName = "substring",
        args = listOf(
            Argument(1, StaticType.STRING, " from "),
            Argument(2, StaticType.INT, " for "),
            Argument(3, StaticType.INT, ")")
        )
    )

    // New syntax
    @Test
    fun substringInvalidArgTypeTest2() = checkInvalidArgType(
        funcName = "substring",
        args = listOf(
            Argument(1, StaticType.STRING, ","),
            Argument(2, StaticType.INT, ","),
            Argument(3, StaticType.INT, ")")
        )
    )

    // The invalid arity check is considered as syntax error and already done in the ParserErrorsTest.kt
}
