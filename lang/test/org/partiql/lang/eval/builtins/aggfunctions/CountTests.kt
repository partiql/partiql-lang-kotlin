package org.partiql.lang.eval.builtins.aggfunctions

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase

class CountTests : EvaluatorTestBase() {
    @Test
    fun countEmpty() = runEvaluatorTestCase("COUNT(`[]`)", expectedResult = "0")

    @Test
    fun countNull() = runEvaluatorTestCase("COUNT([null, null])", expectedResult = "0")

    @Test
    fun countMissing() = runEvaluatorTestCase("COUNT([missing])", expectedResult = "0")

    @Test
    fun countBoolean() = runEvaluatorTestCase("COUNT(`[true, false]`)", expectedResult = "2")

    @Test
    fun countInt() = runEvaluatorTestCase("COUNT(`[1, 2, 3]`)", expectedResult = "3")

    @Test
    fun countDecimal() = runEvaluatorTestCase("COUNT(`[1e0, 2e0, 3e0]`)", expectedResult = "3")

    @Test
    fun countFloat() = runEvaluatorTestCase("COUNT(`[1e0, 2e0, 3e0]`)", expectedResult = "3")

    @Test
    fun countString() = runEvaluatorTestCase("COUNT(`[\"1\", \"2\", \"3\"]`)", expectedResult = "3")

    @Test
    fun countTimestamp() = runEvaluatorTestCase("COUNT(`[2020-01-01T00:00:00Z, 2020-01-01T00:00:01Z]`)", expectedResult = "2")

    @Test
    fun countBlob() = runEvaluatorTestCase("COUNT(`[{{ aaaa }}, {{ aaab }}]`)", expectedResult = "2")

    @Test
    fun countClob() = runEvaluatorTestCase("COUNT(`[{{ \"aaaa\" }}, {{ \"aaab\" }}]`)", expectedResult = "2")

    @Test
    fun countSexp() = runEvaluatorTestCase("COUNT(`[(1), (2)]`)", expectedResult = "2")

    @Test
    fun countList() = runEvaluatorTestCase("COUNT(`[[1], [2]]`)", expectedResult = "2")

    @Test
    fun countBag() = runEvaluatorTestCase("COUNT([<<1>>, <<2>>])", expectedResult = "2")

    @Test
    fun countStruct() = runEvaluatorTestCase("COUNT(`[{'a':1}, {'a':2}]`)", expectedResult = "2")

    @Test
    fun countMixed0() = runEvaluatorTestCase("COUNT([null, missing, 1, 2])", expectedResult = "2")

    @Test
    fun countMixed1() = runEvaluatorTestCase("COUNT([1, '2', true, `2020-01-01T00:00:00Z`, `{{ aaaa }}`])", expectedResult = "5")
}
