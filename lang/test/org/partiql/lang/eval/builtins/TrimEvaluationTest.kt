package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase


class TrimEvaluationTest : EvaluatorTestBase() {

    @Test
    fun trimDefault1() = assertEval("trim('   string   ')", "\"string\"")

    @Test
    fun trimDefault2() = assertEval("trim('   string')", "\"string\"")

    @Test
    fun trimDefault3() = assertEval("trim('string   ')", "\"string\"")

    @Test
    fun trimOtherWhitespaces() = assertEval("trim('\tstring\t')", "\"\tstring\t\"")

    @Test
    fun trimSimpleCaseWithFrom1() = assertEval("trim(from '   string   ')", "\"string\"")

    @Test
    fun trimSimpleCaseWithFrom2() = assertEval("trim(from '   string')", "\"string\"")

    @Test
    fun trimSimpleCaseWithFrom3() = assertEval("trim(from 'string   ')", "\"string\"")

    @Test
    fun trimBoth1() = assertEval("trim(both from '   string   ')", "\"string\"")

    @Test
    fun trimBoth2() = assertEval("trim(both from '   string')", "\"string\"")

    @Test
    fun trimBoth3() = assertEval("trim(both from 'string   ')", "\"string\"")

    @Test
    fun trimLeading1() = assertEval("trim(leading from '   string   ')", "\"string   \"")

    @Test
    fun trimLeading2() = assertEval("trim(leading from '   string')", "\"string\"")

    @Test
    fun trimLeading3() = assertEval("trim(leading from 'string   ')", "\"string   \"")

    @Test
    fun trimTrailing1() = assertEval("trim(trailing from '   string   ')", "\"   string\"")

    @Test
    fun trimTrailing2() = assertEval("trim(trailing from '   string')", "\"   string\"")

    @Test
    fun trimTrailing3() = assertEval("trim(trailing from 'string   ')", "\"string\"")

    @Test
    fun trimMultiple1() = assertEval("trim(both ' -=' from '- =string =-  ')", "\"string\"")

    @Test
    fun trimMultiple2() = assertEval("trim(both ' -=' from '--===    -= -= -=   string')", "\"string\"")

    @Test
    fun trimMultiple3() = assertEval("trim(both ' -=' from 'string ==- = -=- - ----------  ')", "\"string\"")

    @Test
    fun trimMultiple4() = assertEval("trim(both ' ' from '            ')", "\"\"")

    @Test
    fun trimMultiple5() = assertEval("trim(leading ' ' from '            ')", "\"\"")

    @Test
    fun trimMultiple6() = assertEval("trim(trailing ' ' from '            ')", "\"\"")

    @Test
    fun trimEmoji1() = assertEval("trim(both '💩' from  '💩💩💩💩💩💩💩💩💩💩😁😞😸😸💩💩💩💩💩💩💩💩💩💩💩💩💩💩')",
                                  "\"😁😞😸😸\"")

    @Test
    fun trimEmoji2() = assertEval("trim('               😁😞😸😸             ')", "\"😁😞😸😸\"")

    @Test
    fun trimChinese() = assertEval("trim(both '話 ' from '話話 話話話話話話費谷料村能話話話話 話話話話    ')", "\"費谷料村能\"")

    @Test
    fun trimExpression() = assertEval("trim('   a'||'b  ')", "\"ab\"")

    @Test
    fun trimToRemoveExpression() = assertEval("""
            SELECT
                trim(both el from '   1ab1  ') as trimmed
            FROM
                <<' 1'>> AS el
            """, "[{trimmed:\"ab\"}]")

    @Test
    fun trimWrongToRemoveType() =
        checkInputThrowingEvaluationException(
            input = "trim(trailing 1 from '')",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "trim",
                Property.ARGUMENT_POSITION to 2,
                Property.EXPECTED_ARGUMENT_TYPES to "STRING",
                Property.ACTUAL_ARGUMENT_TYPES to "INT",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )

    @Test
    fun trimWrongStringType() =
        checkInputThrowingEvaluationException(
            input = "trim(true)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "trim",
                Property.ARGUMENT_POSITION to 1,
                Property.EXPECTED_ARGUMENT_TYPES to "STRING, or SYMBOL",
                Property.ACTUAL_ARGUMENT_TYPES to "BOOL",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L),
            expectedPermissiveModeResult = "MISSING")


    @Test
    fun trimWrongStringType2() =
        checkInputThrowingEvaluationException(
            input = "trim(trailing from true)",
            errorCode = ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
            expectErrorContextValues = mapOf<Property, Any>(
                Property.FUNCTION_NAME to "trim",
                Property.ARGUMENT_POSITION to 2,
                Property.EXPECTED_ARGUMENT_TYPES to "STRING",
                Property.ACTUAL_ARGUMENT_TYPES to "BOOL",
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L
            ),
            expectedPermissiveModeResult = "MISSING"
        )

    @Test
    fun trimDefaultSpecification1() = assertEval("trim('12' from '1212b1212')", "\"b\"")

    @Test
    fun trimDefaultSpecification2() = assertEval("trim('12' from '1212b')", "\"b\"")

    @Test
    fun trimDefaultSpecification3() = assertEval("trim('12' from 'b1212')", "\"b\"")

    @Test
    fun trimNull01() = assertEval("trim(both null from '')", "null")

    @Test
    fun trimNull02() = assertEval("trim(both '' from null)", "null")

    @Test
    fun trimNull03() = assertEval("trim(null from '')", "null")

    @Test
    fun trimNull04() = assertEval("trim('' from null)", "null")

    @Test
    fun trimNull05() = assertEval("trim(null)", "null")

    @Test
    fun trimMissing01() = assertEval("trim(both missing from '')", "null")

    @Test
    fun trimMissing02() = assertEval("trim(both '' from missing)", "null")

    @Test
    fun trimMissing03() = assertEval("trim(missing from '')", "null")

    @Test
    fun trimMissing04() = assertEval("trim('' from missing)", "null")

    @Test
    fun trimMissing05() = assertEval("trim(missing)", "null")
}