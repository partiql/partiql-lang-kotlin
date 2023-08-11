package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget

class AnySomeTests : EvaluatorTestBase() {
    @Test
    fun anyBagLiterals() = runEvaluatorTestCase(
        query = "ANY(<< false, true, false >>)",
        expectedResult = "true",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun anyListExpressions() = runEvaluatorTestCase(
        query = "ANY([ 1 < 5, false, 5 IS NULL ])",
        expectedResult = "true",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun anySingleTrue() = runEvaluatorTestCase(
        query = "ANY(<<true>>)",
        expectedResult = "true",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun anySingleFalse() = runEvaluatorTestCase(
        query = "ANY([false])",
        expectedResult = "false",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun anyNullsWithTrue() = runEvaluatorTestCase(
        query = "ANY(<< NULL, 2<3, MISSING, false >>)",
        expectedResult = "true",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun anyNullsWithFalse() = runEvaluatorTestCase(
        query = "ANY([ 2>3, NULL ])",
        expectedResult = "false",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun someNullsOnly() = runEvaluatorTestCase(
        query = "SOME([NULL, MISSING])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun someEmpty() = runEvaluatorTestCase(
        query = "SOME(<< >>)",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun someOneNonBools() = runEvaluatorErrorTestCase(
        query = "SOME(<< true, 5, true >>)",
        ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun someAllNonBools() = runEvaluatorErrorTestCase(
        query = "SOME([ 5, 'hello', 3.14])",
        ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun someNestedColl() = runEvaluatorErrorTestCase(
        query = "SOME([[ true, false]])",
        ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )
}
