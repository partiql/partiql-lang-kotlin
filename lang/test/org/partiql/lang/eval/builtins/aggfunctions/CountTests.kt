package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget

class CountTests : EvaluatorTestBase() {
    @Test
    fun countEmpty() =
        runEvaluatorTestCase(query = "COUNT(`[]`)", expectedResult = "0", target = EvaluatorTestTarget.COMPILER_PIPELINE)

    @Test
    fun countNull() = runEvaluatorTestCase(
        query = "COUNT([null, null])",
        expectedResult = "0",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countMissing() =
        runEvaluatorTestCase("COUNT([missing])", expectedResult = "0", target = EvaluatorTestTarget.COMPILER_PIPELINE)

    @Test
    fun countBoolean() = runEvaluatorTestCase(
        query = "COUNT(`[true, false]`)",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countInt() =
        runEvaluatorTestCase("COUNT(`[1, 2, 3]`)", expectedResult = "3", target = EvaluatorTestTarget.COMPILER_PIPELINE)

    @Test
    fun countDecimal() = runEvaluatorTestCase(
        query = "COUNT(`[1e0, 2e0, 3e0]`)",
        expectedResult = "3",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countFloat() = runEvaluatorTestCase(
        query = "COUNT(`[1e0, 2e0, 3e0]`)",
        expectedResult = "3",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countString() = runEvaluatorTestCase(
        query = "COUNT(`[\"1\", \"2\", \"3\"]`)",
        expectedResult = "3",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countTimestamp() = runEvaluatorTestCase(
        query = "COUNT(`[2020-01-01T00:00:00Z, 2020-01-01T00:00:01Z]`)",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countBlob() = runEvaluatorTestCase(
        query = "COUNT(`[{{ aaaa }}, {{ aaab }}]`)",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countClob() = runEvaluatorTestCase(
        query = "COUNT(`[{{ \"aaaa\" }}, {{ \"aaab\" }}]`)",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countSexp() = runEvaluatorTestCase(
        query = "COUNT(`[(1), (2)]`)",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countList() = runEvaluatorTestCase(
        query = "COUNT(`[[1], [2]]`)",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countBag() = runEvaluatorTestCase(
        query = "COUNT([<<1>>, <<2>>])",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countStruct() = runEvaluatorTestCase(
        query = "COUNT(`[{'a':1}, {'a':2}]`)",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countMixed0() = runEvaluatorTestCase(
        query = "COUNT([null, missing, 1, 2])",
        expectedResult = "2",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )

    @Test
    fun countMixed1() = runEvaluatorTestCase(
        query = "COUNT([1, '2', true, `2020-01-01T00:00:00Z`, `{{ aaaa }}`])",
        expectedResult = "5",
        target = EvaluatorTestTarget.COMPILER_PIPELINE
    )
}
