package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import org.junit.*

/**
 * More detailed tests are in [SizeExprFunctionTest]
 */
class SizeEvaluationTest : EvaluatorBase() {

    @Test
    fun emptyStruct() = assertEval("size({})", "0")

    @Test
    fun emptyList() = assertEval("size([])", "0")

    @Test
    fun emptyBag() = assertEval("size(<<>>)", "0")

    @Test
    fun nonEmptyStruct() = assertEval("size(`{ a: 1 }`)", "1")

    @Test
    fun nonEmptyList() = assertEval("size(['foo'])", "1")

    @Test
    fun nonEmptyBag() = assertEval("size(<<'foo'>>)", "1")

    @Test
    fun nullArgument() = assertEval("size(null)", "null")

    @Test
    fun missingArgument() = assertEval("size(missing)", "null")

    @Test
    fun lessArguments() = checkInputThrowingEvaluationException("size()",
                                                                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                mapOf(Property.EXPECTED_ARITY_MIN to 1,
                                                                      Property.EXPECTED_ARITY_MAX to 1,
                                                                      Property.LINE_NUMBER to 1L,
                                                                      Property.COLUMN_NUMBER to 1L))

    @Test
    fun moreArguments() = checkInputThrowingEvaluationException("size(null, null)",
                                                                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                mapOf(Property.EXPECTED_ARITY_MIN to 1,
                                                                      Property.EXPECTED_ARITY_MAX to 1,
                                                                      Property.LINE_NUMBER to 1L,
                                                                      Property.COLUMN_NUMBER to 1L))

    @Test
    fun wrongTypeOfArgument() = checkInputThrowingEvaluationException("size(1)",
                                                                      ErrorCode.EVALUATOR_INCORRECT_TYPE_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                      mapOf(Property.EXPECTED_ARGUMENT_TYPES to "LIST or BAG or STRUCT",
                                                                            Property.ACTUAL_ARGUMENT_TYPES to "INT",
                                                                            Property.FUNCTION_NAME to "size",
                                                                            Property.LINE_NUMBER to 1L,
                                                                            Property.COLUMN_NUMBER to 1L))
}