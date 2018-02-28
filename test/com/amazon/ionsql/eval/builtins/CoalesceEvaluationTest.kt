package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import org.junit.*

class CoalesceEvaluationTest : EvaluatorBase() {
    @Test fun nullSingleArgument() = assertEval("coalesce(null)", "null")

    @Test fun nullMultipleArgument() = assertEval("coalesce(null, null)", "null")

    @Test fun missingSingleArgument() = assertEval("coalesce(missing)", "null")

    @Test fun missingMultipleArgument() = assertEval("coalesce(missing, missing)", "null")

    @Test fun singleArgument() = assertEval("coalesce(1)", "1")

    @Test fun nullAfterValue() = assertEval("coalesce(1, null)", "1")

    @Test fun valueAfterNull() = assertEval("coalesce(null, 2)", "2")

    @Test fun missingAfterValue() = assertEval("coalesce(1, missing)", "1")

    @Test fun valueAfterMissing() = assertEval("coalesce(missing, 2)", "2")

    @Test fun nullThenMissing() = assertEval("coalesce(null, missing)", "null")

    @Test fun missingThenNull() = assertEval("coalesce(missing, null)", "null")

    @Test fun severalArgumentsNullAndMissingMixed() = assertEval("coalesce(null, missing, null, null, 2, 3, 4, 5)", "2")

    @Test fun severalNullArguments() = assertEval("coalesce(null, null, 2, 3, 4, 5)", "2")

    @Test fun severalMissingArguments() = assertEval("coalesce(missing, missing, 2, 3, 4, 5)", "2")

    @Test
    fun zeroArguments() = checkInputThrowingEvaluationException(input = "coalesce()",
                                                                errorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                                                                expectErrorContextValues = sourceLocationProperties(1, 1) +
                                                                                           mapOf(Property.EXPECTED_ARITY_MIN to 1,
                                                                                                 Property.EXPECTED_ARITY_MAX to Int.MAX_VALUE))
}