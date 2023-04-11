package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget

class EveryTests : EvaluatorTestBase() {
    @Test
    fun everyBagLiterals() = runEvaluatorTestCase(
        query = "EVERY(<< true, false, true >>)",
        expectedResult = "false",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyListExpressions() = runEvaluatorTestCase(
        query = "EVERY([ 1 < 5, true, NULL IS NULL])",
        expectedResult = "true",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everySingleTrue() = runEvaluatorTestCase(
        query = "EVERY(<<true>>)",
        expectedResult = "true",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everySingleFalse() = runEvaluatorTestCase(
        query = "EVERY([false])",
        expectedResult = "false",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyNullsWithTrue() = runEvaluatorTestCase(
        query = "EVERY(<< NULL, 2<3, MISSING, true >>)",
        expectedResult = "true",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyNullsWithFalse() = runEvaluatorTestCase(
        query = "EVERY([ 2>3, NULL ])",
        expectedResult = "false",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyNullsOnly() = runEvaluatorTestCase(
        query = "EVERY([NULL, MISSING])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyEmpty() = runEvaluatorTestCase(
        query = "EVERY(<< >>)",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyOneNonBools() = runEvaluatorErrorTestCase(
        query = "EVERY(<< true, 5, true >>)",
        ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyAllNonBools() = runEvaluatorErrorTestCase(
        query = "EVERY([ 5, 'hello', 3.14])",
        ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun everyNestedColl() = runEvaluatorErrorTestCase(
        query = "EVERY([[ true, false]])",
        ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )
}
