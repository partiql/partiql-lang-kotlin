package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget

class MinTests : EvaluatorTestBase() {
    @Test
    fun minNull() = runEvaluatorTestCase(
        query = "min([null, null])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minMissing() = runEvaluatorTestCase(
        query = "min([missing, missing])",
        expectedResult = "null",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minNumber0() = runEvaluatorTestCase(
        query = "min(`[1, 2, 3]`)",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minNumber1() = runEvaluatorTestCase(
        query = "min(`[1, 2.0, 3]`)",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minNumber2() = runEvaluatorTestCase(
        query = "min(`[1, 2e0, 3e0]`)",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minNumber3() = runEvaluatorTestCase(
        query = "min(`[1, 2d0, 3d0]`)",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minNumber4() = runEvaluatorTestCase(
        query = "min(`[1, 2e0, 3d0]`)",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minNumber5() = runEvaluatorTestCase(
        query = "min(`[1, 2d0, 3e0]`)",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minString0() = runEvaluatorTestCase(
        query = "min(['a', 'abc', '3'])",
        expectedResult = "'3'",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minString1() = runEvaluatorTestCase(
        query = "min(['1', '2', '3', null])",
        expectedResult = "'1'",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minTimestamp0() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minTimestamp1() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, `2020-01-01T00:01:00Z`, `2020-01-01T00:02:00Z`])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minTimestamp2() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, `2020-01-01T01:00:00Z`, `2020-01-01T02:00:00Z`])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minTimestamp3() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, `2020-01-02T00:00:00Z`, `2020-01-03T00:00:00Z`])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minTimestamp4() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, `2020-02-01T00:00:00Z`, `2020-03-01T00:00:00Z`])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minTimestamp5() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, `2021-01-01T00:00:00Z`, `2022-01-01T00:00:00Z`])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minTimestamp6() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`, null])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minBoolean() = runEvaluatorTestCase(
        query = "min([false, true])",
        expectedResult = "false",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minBlob() = runEvaluatorTestCase(
        query = "min([`{{ aaaa }}`, `{{ aaab }}`])",
        expectedResult = "`{{aaaa}}`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minClob() = runEvaluatorTestCase(
        query = "min([`{{\"a\"}}`, `{{\"b\"}}`])",
        expectedResult = "`{{\"a\"}}`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minMixed0() = runEvaluatorTestCase(
        query = "min([false, 1])",
        expectedResult = "false",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minMixed1() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, 1])",
        expectedResult = "1",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minMixed2() = runEvaluatorTestCase(
        query = "min([`2020-01-01T00:00:00Z`, '1'])",
        expectedResult = "`2020-01-01T00:00:00Z`",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun minMixed3() = runEvaluatorTestCase(
        query = "min([`{{\"abcd\"}}`, '1'])",
        expectedResult = "'1'",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )
}
