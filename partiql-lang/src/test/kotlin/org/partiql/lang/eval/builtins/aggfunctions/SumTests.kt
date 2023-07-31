package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget

class SumTests : EvaluatorTestBase() {
    @Test
    fun sumNull() = runEvaluatorTestCase(
        query = "SUM([null, null])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sumMissing() = runEvaluatorTestCase(
        query = "SUM([missing, missing])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sum0() = runEvaluatorTestCase(
        query = "SUM(`[1, 2, 3]`)",
        expectedResult = "6",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sum1() = runEvaluatorTestCase(
        query = "SUM(`[1, 2e0, 3e0]`)",
        expectedResult = "6e0",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sum2() = runEvaluatorTestCase(
        query = "SUM(`[1, 2d0, 3d0]`)",
        expectedResult = "6d0",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sum3() = runEvaluatorTestCase(
        query = "SUM(`[1, 2e0, 3d0]`)",
        expectedResult = "6d0",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sum4() = runEvaluatorTestCase(
        query = "SUM(`[1, 2d0, 3e0]`)",
        expectedResult = "6d0",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sumOverflow() = runEvaluatorErrorTestCase(
        query = "SUM([1, 9223372036854775807])",
        expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun sumUnderflow() = runEvaluatorErrorTestCase(
        query = "SUM([-1, -9223372036854775808])",
        expectedErrorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )
}
