package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase

class MaxTests : EvaluatorTestBase() {
    @Test
    fun maxNull() = runEvaluatorTestCase("max([null, null])", expectedResult = "null")

    @Test
    fun maxMissing() = runEvaluatorTestCase("max([missing, missing])", expectedResult = "null")

    @Test
    fun maxNumber0() = runEvaluatorTestCase("max(`[1, 2, 3]`)", expectedResult = "3")

    @Test
    fun maxNumber1() = runEvaluatorTestCase("max(`[1, 2.0, 3]`)", expectedResult = "3")

    @Test
    fun maxNumber2() = runEvaluatorTestCase("max(`[1, 2e0, 3e0]`)", expectedResult = "3e0")

    @Test
    fun maxNumber3() = runEvaluatorTestCase("max(`[1, 2d0, 3d0]`)", expectedResult = "3d0")

    @Test
    fun maxNumber4() = runEvaluatorTestCase("max(`[1, 2e0, 3d0]`)", expectedResult = "3d0")

    @Test
    fun maxNumber5() = runEvaluatorTestCase("max(`[1, 2d0, 3e0]`)", expectedResult = "3e0")

    @Test
    fun maxString0() = runEvaluatorTestCase("max(['a', 'abc', '3'])", expectedResult = "\"abc\"")

    @Test
    fun maxString1() = runEvaluatorTestCase("max(['1', '2', '3', null])", expectedResult = "\"3\"")

    @Test
    fun maxTimestamp0() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`])", expectedResult = "2020-01-01T00:00:02Z")

    @Test
    fun maxTimestamp1() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T00:01:00Z`, `2020-01-01T00:02:00Z`])", expectedResult = "2020-01-01T00:02:00Z")

    @Test
    fun maxTimestamp2() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T01:00:00Z`, `2020-01-01T02:00:00Z`])", expectedResult = "2020-01-01T02:00:00Z")

    @Test
    fun maxTimestamp3() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-02T00:00:00Z`, `2020-01-03T00:00:00Z`])", expectedResult = "2020-01-03T00:00:00Z")

    @Test
    fun maxTimestamp4() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-02-01T00:00:00Z`, `2020-03-01T00:00:00Z`])", expectedResult = "2020-03-01T00:00:00Z")

    @Test
    fun maxTimestamp5() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2021-01-01T00:00:00Z`, `2022-01-01T00:00:00Z`])", expectedResult = "2022-01-01T00:00:00Z")

    @Test
    fun maxTimestamp6() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, `2020-01-01T00:00:01Z`, `2020-01-01T00:00:02Z`, null])", expectedResult = "2020-01-01T00:00:02Z")

    @Test
    fun maxBoolean() = runEvaluatorTestCase("max([false, true])", expectedResult = "true")

    @Test
    fun maxBlob() = runEvaluatorTestCase("max([`{{ aaaa }}`, `{{ aaab }}`])", expectedResult = "{{aaab}}")

    @Test
    fun maxClob() = runEvaluatorTestCase("max([`{{\"a\"}}`, `{{\"b\"}}`])", expectedResult = "{{\"b\"}}")

    @Test
    fun maxMixed0() = runEvaluatorTestCase("max([false, 1])", expectedResult = "1")

    @Test
    fun maxMixed1() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, 1])", expectedResult = "2020-01-01T00:00:00Z")

    @Test
    fun maxMixed2() = runEvaluatorTestCase("max([`2020-01-01T00:00:00Z`, '1'])", expectedResult = "\"1\"")

    @Test
    fun maxMixed3() = runEvaluatorTestCase("max([`{{\"abcd\"}}`, '1'])", expectedResult = "{{\"abcd\"}}")
}
