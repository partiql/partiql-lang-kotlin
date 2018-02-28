package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import org.junit.*

class NullIfEvaluationTest : EvaluatorBase() {
    private val errorContext = sourceLocationProperties(1, 1) + mapOf(Property.EXPECTED_ARITY_MIN to 2,
                                                                      Property.EXPECTED_ARITY_MAX to 2)

    @Test fun sameValue() = assertEval("nullif(1, 1)", "null")

    @Test fun differentValues() = assertEval("nullif(1, 2)", "1")

    @Test fun differentNumberTypes() = assertEval("nullif(1, 1.0)", "null")

    @Test fun differentTypes() = assertEval("nullif(1, '1')", "1")

    @Test fun nullAndNull() = assertEval("nullif(null, null)", "null")

    @Test fun nullAndValue() = assertEval("nullif(null, 1)", "null")

    @Test fun valueAndNull() = assertEval("nullif(1, null)", "1")

    @Test fun nullAndMissing() = assertEval("nullif(null, missing)", "null")

    @Test fun missingAndNull() = assertEval("nullif(missing, null)", "null")

    @Test fun withLists() = assertEval("nullif([], [])", "null")

    @Test fun withStructs() = assertEval("nullif({}, {})", "null")

    @Test fun withStructsAndLists() = assertEval("nullif({}, [])", "{}")

    @Test
    fun zeroArguments() = checkInputThrowingEvaluationException(input = "nullif()",
                                                                errorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                expectErrorContextValues = errorContext)

    @Test
    fun oneArguments() = checkInputThrowingEvaluationException(input = "nullif(1)",
                                                                errorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                expectErrorContextValues = errorContext)

    @Test
    fun threeArguments() = checkInputThrowingEvaluationException(input = "nullif(1, 1, 1)",
                                                                errorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                expectErrorContextValues = errorContext)
}