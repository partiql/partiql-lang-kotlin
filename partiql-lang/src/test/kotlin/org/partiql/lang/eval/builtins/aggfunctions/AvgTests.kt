package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget

class AvgTests : EvaluatorTestBase() {
    @Test
    fun avgNull() = runEvaluatorTestCase(
        query = "AVG([null, null])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgMissing() = runEvaluatorTestCase(
        query = "AVG([missing, missing])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgInt() = runEvaluatorTestCase(
        query = "AVG(`[1, 2, 3]`)",
        expectedResult = "2.",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgMixed0() = runEvaluatorTestCase(
        query = "AVG(`[1, 2e0, 3e0]`)",
        expectedResult = "2.",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgMixed1() = runEvaluatorTestCase(
        query = "AVG(`[1, 2d0, 3d0]`)",
        expectedResult = "2.",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgMixed2() = runEvaluatorTestCase(
        query = "AVG(`[1, 2e0, 3d0]`)",
        expectedResult = "2.",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgMixed3() = runEvaluatorTestCase(
        query = "AVG(`[1, 2d0, 3e0]`)",
        expectedResult = "2.",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgOverflow() = runEvaluatorErrorTestCase(
        query = "AVG([1, 9223372036854775807])",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun avgUnderflow() = runEvaluatorErrorTestCase(
        query = "AVG([-1, -9223372036854775808])",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )
}
