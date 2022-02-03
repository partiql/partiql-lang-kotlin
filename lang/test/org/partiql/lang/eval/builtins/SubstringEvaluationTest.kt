package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.util.to

class SubstringEvaluationTest : EvaluatorTestBase() {
    private fun checkSubstringThrowingEvaluationException(input: String, expectErrorContextValues: Map<Property, Any> = mapOf()) =
        checkInputThrowingEvaluationException(
            input = input,
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.FUNCTION_NAME to "substring"
            ) + expectErrorContextValues,
            expectedPermissiveModeResult = "MISSING"
        )

    // Old syntax: SUBSTRING(<str> FROM <start pos> [FOR <length>])
    @Test fun substringOldSyntax_1() = assertEval("substring('abcdefghi' from 0)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_2() = assertEval("substring('abcdefghi' from 1)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_3() = assertEval("substring('abcdefghi' from -1)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_4() = assertEval("substring('abcdefghi' from 3)", "\"cdefghi\"")
    @Test fun substringOldSyntax_5() = assertEval("substring('abcdefghi' from 3 for 20)", "\"cdefghi\"")
    @Test fun substringOldSyntax_6() = assertEval("substring('1234567890' from 10)", "\"0\"")
    @Test fun substringOldSyntax_7() = assertEval("substring('1234567890' from 11)", "\"\"")
    @Test fun substringOldSyntax_8() = assertEval("substring('1234567890' from 10 for 10)", "\"0\"")
    @Test fun substringOldSyntax_9() = assertEval("substring('1234567890' from 11 for 10)", "\"\"")
    @Test fun substringOldSyntax_10() = assertEval("substring('abcdefghi' from 3 for 4)", "\"cdef\"")
    @Test fun substringOldSyntax_11() = assertEval("substring('abcdefghi' from -1 for 4)", "\"ab\"")
    @Test fun substringOldSyntax_12() = assertEval("substring('abcdefghi' from 1 for 1)", "\"a\"")

    @Test fun substringOldSyntaxUnicode_1() = assertEval("substring('😁😞😸😸' from 2 for 2)", "\"😞😸\"")
    @Test fun substringOldSyntaxUnicode_2() = assertEval("substring('話家身圧費谷料村能計税金' from 3 for 5)", "\"身圧費谷料\"")
    @Test fun substringOldSyntaxUnicode_3() = assertEval("substring('話家身圧費谷料村能計税金' from -3 for 6)", "\"話家\"")

    //Note:  U+0832 is a "combining diacritical mark" https://en.wikipedia.org/wiki/Combining_character.
    //Even though it is visually merged with the preceding letter when displayed, it still counts as a distinct codepoint.
    @Test fun substringOldSyntaxUnicode_4() = assertEval("substring('abcde\u0832fgh' from 3 for 6)", "\"cde\u0832fg\"")

    @Test
    fun substringOldSyntaxError_1() =
        checkSubstringThrowingEvaluationException(
            input = "substring(123456789 from 1 for 3)",
            expectErrorContextValues = mapOf(
                Property.EXPECTED_ARGUMENT_TYPES to "STRING",
                Property.ARGUMENT_POSITION to 1,
                Property.ACTUAL_ARGUMENT_TYPES to "INT"
            )
        )
    @Test
    fun substringOldSyntaxError_2() =
        checkInputThrowingEvaluationException(
            input = "substring('abcdefghi' from 1 for -1)",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            expectErrorContextValues = mapOf(),
            expectedPermissiveModeResult = "MISSING")
    @Test
    fun substringOldSyntaxError_3() =
        checkSubstringThrowingEvaluationException(
            input = "substring('abcdefghi' from 1.0)",
            expectErrorContextValues = mapOf(
                Property.EXPECTED_ARGUMENT_TYPES to "INT",
                Property.ARGUMENT_POSITION to 2,
                Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL"
            )
        )

    @Test
    fun substringOldSyntaxError_4() = checkSubstringThrowingEvaluationException(
        input = "substring('abcdefghi' from 1.0 for 1)",
        expectErrorContextValues = mapOf(
            Property.EXPECTED_ARGUMENT_TYPES to "INT",
            Property.ARGUMENT_POSITION to 2,
            Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL"
        )
    )
    @Test
    fun substringOldSyntaxError_5() = checkSubstringThrowingEvaluationException(
        input = "substring('abcdefghi' from 1 for 1.0)",
        expectErrorContextValues = mapOf(
            Property.EXPECTED_ARGUMENT_TYPES to "INT",
            Property.ARGUMENT_POSITION to 3,
            Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL"
        )
    )
    @Test
    fun substringOldSyntaxError_6() = checkSubstringThrowingEvaluationException(
        input = "substring('abcdefghi' from 1 for '1')",
        expectErrorContextValues = mapOf(
            Property.EXPECTED_ARGUMENT_TYPES to "INT",
            Property.ARGUMENT_POSITION to 3,
            Property.ACTUAL_ARGUMENT_TYPES to "STRING"
        )
    )
    @Test
    fun substringOldSyntaxError_7() = checkSubstringThrowingEvaluationException(
        input = "substring('abcdefghi' from '1')",
        expectErrorContextValues = mapOf(
            Property.EXPECTED_ARGUMENT_TYPES to "INT",
            Property.ARGUMENT_POSITION to 2,
            Property.ACTUAL_ARGUMENT_TYPES to "STRING"
        )
    )

    @Test fun substringOldSyntaxNullArg_1() = assertEval("substring(null from 1)", "null")
    @Test fun substringOldSyntaxNullArg_2() = assertEval("substring('abc' from null)", "null")
    @Test fun substringOldSyntaxNullArg_3() = assertEval("substring(null from 1 for 1)", "null")
    @Test fun substringOldSyntaxNullArg_4() = assertEval("substring('abc' from null for 1)", "null")
    @Test fun substringOldSyntaxNullArg_5() = assertEval("substring('abc' from 1 for null)", "null")

    @Test fun substringOldSyntaxMissingArg_1() = assertEval("substring(missing from 1)", "null")
    @Test fun substringOldSyntaxMissingArg_2() = assertEval("substring('abc' from missing)", "null")
    @Test fun substringOldSyntaxMissingArg_3() = assertEval("substring(missing from 1 for 1)", "null")
    @Test fun substringOldSyntaxMissingArg_4() = assertEval("substring('abc' from missing for 1)", "null")
    @Test fun substringOldSyntaxMissingArg_5() = assertEval("substring('abc' from 1 for missing)", "null")

    @Test fun substringOldSyntaxEmpty_1() = assertEval("substring('' from -1)", "\"\"")
    @Test fun substringOldSyntaxEmpty_2() = assertEval("substring('' from 0)", "\"\"")
    @Test fun substringOldSyntaxEmpty_3() = assertEval("substring('' from 99)", "\"\"")
    @Test fun substringOldSyntaxEmpty_4() = assertEval("substring('' from -1 for 999)", "\"\"")
    @Test fun substringOldSyntaxEmpty_5() = assertEval("substring('' from 0 for 999)", "\"\"")
    @Test fun substringOldSyntaxEmpty_6() = assertEval("substring('' from -4 for 1)", "\"\"")
    @Test fun substringOldSyntaxBefore() = assertEval("substring('1' from -4 for 1)", "\"\"")

    //Same as above, with the new syntax: SUBSTRING(<str>, <start pos> [, <length>])
    @Test fun substringNewSyntax_1() = assertEval("substring('abcdefghi', 0)", "\"abcdefghi\"")
    @Test fun substringNewSyntax_2() = assertEval("substring('abcdefghi', 1)", "\"abcdefghi\"")
    @Test fun substringNewSyntax_3() = assertEval("substring('abcdefghi', -1)", "\"abcdefghi\"")
    @Test fun substringNewSyntax_4() = assertEval("substring('abcdefghi', 3)", "\"cdefghi\"")
    @Test fun substringNewSyntax_5() = assertEval("substring('abcdefghi', 3, 20)", "\"cdefghi\"")
    @Test fun substringNewSyntax_6() = assertEval("substring('1234567890', 10)", "\"0\"")
    @Test fun substringNewSyntax_7() = assertEval("substring('1234567890', 11)", "\"\"")
    @Test fun substringNewSyntax_8() = assertEval("substring('1234567890', 10, 10)", "\"0\"")
    @Test fun substringNewSyntax_9() = assertEval("substring('1234567890', 11, 10)", "\"\"")
    @Test fun substringNewSyntax_10() = assertEval("substring('abcdefghi', 3, 4)", "\"cdef\"")
    @Test fun substringNewSyntax_11() = assertEval("substring('abcdefghi', -1, 4)", "\"ab\"")
    @Test fun substringNewSyntax_12() = assertEval("substring('abcdefghi', 1, 1)", "\"a\"")

    @Test fun substringNewSyntaxUnicode_1() = assertEval("substring('😁😞😸😸', 2, 2)", "\"😞😸\"")
    @Test fun substringNewSyntaxUnicode_2() = assertEval("substring('話家身圧費谷料村能計税金', 3, 5)", "\"身圧費谷料\"")
    @Test fun substringNewSyntaxUnicode_3() = assertEval("substring('話家身圧費谷料村能計税金', -3, 6)", "\"話家\"")
    @Test fun substringNewSyntaxUnicode_4() = assertEval("substring('abcde\u0832fgh', 3, 6)", "\"cde\u0832fg\"")

    @Test
    fun substringNewSyntaxError_1() =
        checkSubstringThrowingEvaluationException(
            input = "substring(123456789, 1, 3)",
            expectErrorContextValues = mapOf(
                Property.EXPECTED_ARGUMENT_TYPES to "STRING",
                Property.ARGUMENT_POSITION to 1,
                Property.ACTUAL_ARGUMENT_TYPES to "INT"
            ))
    @Test
    fun substringNewSyntaxError_2() =
        checkInputThrowingEvaluationException(
            input = "substring('abcdefghi', 1, -1)",
            errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_FUNC_CALL,
            expectErrorContextValues = mapOf(),
            expectedPermissiveModeResult = "MISSING")
    @Test
    fun substringNewSyntaxError_3() =
        checkSubstringThrowingEvaluationException(
            input = "substring('abcdefghi', 1.0)",
            expectErrorContextValues = mapOf(
                Property.EXPECTED_ARGUMENT_TYPES to "INT",
                Property.ARGUMENT_POSITION to 2,
                Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL"
            ))
    @Test
    fun substringNewSyntaxError_4() =
        checkSubstringThrowingEvaluationException(
            input = "substring('abcdefghi', 1.0, 1)",
            expectErrorContextValues = mapOf(
                Property.EXPECTED_ARGUMENT_TYPES to "INT",
                Property.ARGUMENT_POSITION to 2,
                Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL"
            ))
    @Test
    fun substringNewSyntaxError_5() =
        checkSubstringThrowingEvaluationException(
            input = "substring('abcdefghi', 1, 1.0)",
            expectErrorContextValues = mapOf(
                Property.EXPECTED_ARGUMENT_TYPES to "INT",
                Property.ARGUMENT_POSITION to 3,
                Property.ACTUAL_ARGUMENT_TYPES to "DECIMAL"
            ))
    @Test
    fun substringNewSyntaxError_6() = checkSubstringThrowingEvaluationException(
        input = "substring('abcdefghi',1,'1')",
        expectErrorContextValues = mapOf(
            Property.EXPECTED_ARGUMENT_TYPES to "INT",
            Property.ARGUMENT_POSITION to 3,
            Property.ACTUAL_ARGUMENT_TYPES to "STRING"
        )
    )
    @Test
    fun substringNewSyntaxError_7() = checkSubstringThrowingEvaluationException(
        input = "substring('abcdefghi','1')",
        expectErrorContextValues = mapOf(
            Property.EXPECTED_ARGUMENT_TYPES to "INT",
            Property.ARGUMENT_POSITION to 2,
            Property.ACTUAL_ARGUMENT_TYPES to "STRING"
        )
    )

    @Test fun substringNewSyntaxNullArg_1() = assertEval("substring(null, 1)", "null")
    @Test fun substringNewSyntaxNullArg_2() = assertEval("substring('abc', null)", "null")
    @Test fun substringNewSyntaxNullArg_3() = assertEval("substring(null, 1, 1)", "null")
    @Test fun substringNewSyntaxNullArg_4() = assertEval("substring('abc', null, 1)", "null")
    @Test fun substringNewSyntaxNullArg_5() = assertEval("substring('abc', 1, null)", "null")

    @Test fun substringNewSyntaxMissingArg_1() = assertEval("substring(missing, 1)", "null")
    @Test fun substringNewSyntaxMissingArg_2() = assertEval("substring('abc', missing)", "null")
    @Test fun substringNewSyntaxMissingArg_3() = assertEval("substring(missing, 1, 1)", "null")
    @Test fun substringNewSyntaxMissingArg_4() = assertEval("substring('abc', missing, 1)", "null")
    @Test fun substringNewSyntaxMissingArg_5() = assertEval("substring('abc', 1, missing)", "null")

    @Test fun substringNewSyntaxEmpty_1() = assertEval("substring('' from -1)", "\"\"")
    @Test fun substringNewSyntaxEmpty_2() = assertEval("substring('' from 0)", "\"\"")
    @Test fun substringNewSyntaxEmpty_3() = assertEval("substring('' from 99)", "\"\"")
    @Test fun substringNewSyntaxEmpty_4() = assertEval("substring('' from -1 for 999)", "\"\"")
    @Test fun substringNewSyntaxEmpty_5() = assertEval("substring('' from 0 for 999)", "\"\"")
    @Test fun substringNewSyntaxEmpty_6() = assertEval("substring('' from -4 for 1)", "\"\"")
    @Test fun substringNewSyntaxBefore() = assertEval("substring('1' from -4 for 1)", "\"\"")
}
