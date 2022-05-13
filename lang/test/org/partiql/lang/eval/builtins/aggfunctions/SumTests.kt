package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase

class SumTests : EvaluatorTestBase() {
    @Test
    fun sumNull() = runEvaluatorTestCase("SUM([null, null])", expectedResult = "null")

    @Test
    fun sumMissing() = runEvaluatorTestCase("SUM([missing, missing])", expectedResult = "null")

    @Test
    fun sum0() = runEvaluatorTestCase("SUM(`[1, 2, 3]`)", expectedResult = "6")

    @Test
    fun sum1() = runEvaluatorTestCase("SUM(`[1, 2e0, 3e0]`)", expectedResult = "6e0")

    @Test
    fun sum2() = runEvaluatorTestCase("SUM(`[1, 2d0, 3d0]`)", expectedResult = "6d0")

    @Test
    fun sum3() = runEvaluatorTestCase("SUM(`[1, 2e0, 3d0]`)", expectedResult = "6d0")

    @Test
    fun sum4() = runEvaluatorTestCase("SUM(`[1, 2d0, 3e0]`)", expectedResult = "6d0")

    @Test
    fun sumOverflow() = runEvaluatorErrorTestCase(
        "SUM([1, 9223372036854775807])",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun sumUnderflow() = runEvaluatorErrorTestCase(
        "SUM([-1, -9223372036854775808])",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedPermissiveModeResult = "MISSING"
    )
}
