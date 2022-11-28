package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget

class MaxTests : EvaluatorTestBase() {
    @Test
    fun maxNull() = runEvaluatorTestCase(
        query = "max([null, null])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxMissing() = runEvaluatorTestCase(
        query = "max([missing, missing])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxNumber0() = runEvaluatorTestCase(
        query = "max(`[1, 2, 3]`)",
        expectedResult = "3",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxNumber1() = runEvaluatorTestCase(
        query = "max(`[1, 2.0, 3]`)",
        expectedResult = "3",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxNumber2() = runEvaluatorTestCase(
        query = "max(`[1, 2e0, 3e0]`)",
        expectedResult = "`3e0`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxNumber3() = runEvaluatorTestCase(
        query = "max(`[1, 2d0, 3d0]`)",
        expectedResult = "`3d0`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxNumber4() = runEvaluatorTestCase(
        query = "max(`[1, 2e0, 3d0]`)",
        expectedResult = "`3d0`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxNumber5() = runEvaluatorTestCase(
        query = "max(`[1, 2d0, 3e0]`)",
        expectedResult = "`3e0`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxString0() = runEvaluatorTestCase(
        query = "max(['a', 'abc', '3'])",
        expectedResult = "'abc'",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxString1() = runEvaluatorTestCase(
        query = "max(['1', '2', '3', null])",
        expectedResult = "'3'",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxTimestamp0() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`])",
        expectedResult = "`2020-01-01T00:00:02Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxTimestamp1() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, `2020-01-01T00:01:00Z`, `2020-01-01T00:02:00Z`])",
        expectedResult = "`2020-01-01T00:02:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxTimestamp2() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, `2020-01-01T01:00:00Z`, `2020-01-01T02:00:00Z`])",
        expectedResult = "`2020-01-01T02:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxTimestamp3() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, `2020-01-02T00:00:00Z`, `2020-01-03T00:00:00Z`])",
        expectedResult = "`2020-01-03T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxTimestamp4() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, `2020-02-01T00:00:00Z`, `2020-03-01T00:00:00Z`])",
        expectedResult = "`2020-03-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxTimestamp5() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, `2021-01-01T00:00:00Z`, `2022-01-01T00:00:00Z`])",
        expectedResult = "`2022-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxTimestamp6() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`, null])",
        expectedResult = "`2020-01-01T00:00:02Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxBoolean() = runEvaluatorTestCase(
        query = "max([false, true])",
        expectedResult = "TRUE",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxBlob() = runEvaluatorTestCase(
        query = "max([`{{ aaaa }}`, `{{ aaab }}`])",
        expectedResult = "`{{aaab}}`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxClob() = runEvaluatorTestCase(
        query = "max([`{{\"a\"}}`, `{{\"b\"}}`])",
        expectedResult = "`{{\"b\"}}`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxMixed0() = runEvaluatorTestCase(
        query = "max([false, 1])",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxMixed1() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, 1])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxMixed2() = runEvaluatorTestCase(
        query = "max([`2020-01-01T00:00:00Z`, '1'])",
        expectedResult = "'1'",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun maxMixed3() = runEvaluatorTestCase(
        query = "max([`{{\"abcd\"}}`, '1'])",
        expectedResult = "`{{\"abcd\"}}`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )
}
