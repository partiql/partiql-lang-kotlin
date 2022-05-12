package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase

class AvgTests : EvaluatorTestBase() {
    @Test
    fun avgNull() = runEvaluatorTestCase("AVG([null, null])", expectedResult = "null")

    @Test
    fun avgMissing() = runEvaluatorTestCase("AVG([missing, missing])", expectedResult = "null")

    @Test
    fun avgInt() = runEvaluatorTestCase("AVG(`[1, 2, 3]`)", expectedResult = "2.")

    @Test
    fun avgMixed0() = runEvaluatorTestCase("AVG(`[1, 2e0, 3e0]`)", expectedResult = "2.")

    @Test
    fun avgMixed1() = runEvaluatorTestCase("AVG(`[1, 2d0, 3d0]`)", expectedResult = "2.")

    @Test
    fun avgMixed2() = runEvaluatorTestCase("AVG(`[1, 2e0, 3d0]`)", expectedResult = "2.")

    @Test
    fun avgMixed3() = runEvaluatorTestCase("AVG(`[1, 2d0, 3e0]`)", expectedResult = "2.")

    @Test
    fun avgOverflow() = runEvaluatorErrorTestCase(
        "AVG([1, 9223372036854775807])",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun avgUnderflow() = runEvaluatorErrorTestCase(
        "AVG([-1, -9223372036854775808])",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING"
    )
}
