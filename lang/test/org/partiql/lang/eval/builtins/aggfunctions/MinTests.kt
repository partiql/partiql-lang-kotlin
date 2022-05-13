package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase

class MinTests : EvaluatorTestBase() {
    @Test
    fun minNull() = runEvaluatorTestCase("min([null, null])", expectedResult = "null")

    @Test
    fun minMissing() = runEvaluatorTestCase("min([missing, missing])", expectedResult = "null")

    @Test
    fun minNumber0() = runEvaluatorTestCase("min(`[1, 2, 3]`)", expectedResult = "1")

    @Test
    fun minNumber1() = runEvaluatorTestCase("min(`[1, 2.0, 3]`)", expectedResult = "1")

    @Test
    fun minNumber2() = runEvaluatorTestCase("min(`[1, 2e0, 3e0]`)", expectedResult = "1")

    @Test
    fun minNumber3() = runEvaluatorTestCase("min(`[1, 2d0, 3d0]`)", expectedResult = "1")

    @Test
    fun minNumber4() = runEvaluatorTestCase("min(`[1, 2e0, 3d0]`)", expectedResult = "1")

    @Test
    fun minNumber5() = runEvaluatorTestCase("min(`[1, 2d0, 3e0]`)", expectedResult = "1")

    @Test
    fun minString0() = runEvaluatorTestCase("min(['a', 'abc', '3'])", expectedResult = "\"3\"")

    @Test
    fun minString1() = runEvaluatorTestCase("min(['1', '2', '3', null])", expectedResult = "\"1\"")

    @Test
    fun minTimestamp0() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minTimestamp1() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T00:01:00Z`, `2020-01-01T00:02:00Z`])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minTimestamp2() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T01:00:00Z`, `2020-01-01T02:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minTimestamp3() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-02T00:00:00Z`, `2020-01-03T00:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minTimestamp4() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-02-01T00:00:00Z`, `2020-03-01T00:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minTimestamp5() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2021-01-01T00:00:00Z`, `2022-01-01T00:00:00Z`])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minTimestamp6() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`, null])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minBoolean() = runEvaluatorTestCase("min([false, true])", expectedResult = "false")

    @Test
    fun minBlob() = runEvaluatorTestCase("min([`{{ aaaa }}`, `{{ aaab }}`])", expectedResult = "{{aaaa}}")

    @Test
    fun minClob() = runEvaluatorTestCase("min([`{{\"a\"}}`, `{{\"b\"}}`])", expectedResult = "{{\"a\"}}")

    @Test
    fun minMixed0() = runEvaluatorTestCase("min([false, 1])", expectedResult = "false")

    @Test
    fun minMixed1() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, 1])", expectedResult = "1")

    @Test
    fun minMixed2() = runEvaluatorTestCase("min([`2020-01-01T00:00:00Z`, '1'])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun minMixed3() = runEvaluatorTestCase("min([`{{\"abcd\"}}`, '1'])", expectedResult = "\"1\"")
}
