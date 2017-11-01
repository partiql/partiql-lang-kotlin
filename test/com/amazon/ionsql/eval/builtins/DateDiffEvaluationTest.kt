package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.eval.*
import org.junit.*

/**
 * More detailed tests are in [DateDiffExprFunctionTest] and parsing related tests in [IonSqlParserTest] and
 * [ParserErrorsTest]
 */
class DateDiffEvaluationTest : EvaluatorBase() {

    @Test
    fun dateDiffYear() = assertEval("date_diff(year, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "1")

    @Test
    fun dateDiffMonth() = assertEval("date_diff(month, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "12")

    @Test
    fun dateDiffDay() = assertEval("date_diff(day, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "366")

    @Test
    fun dateDiffHour() = assertEval("date_diff(hour, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "8784")

    @Test
    fun dateDiffMinute() = assertEval("date_diff(minute, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "527040")

    @Test
    fun dateDiffSecond() = assertEval("date_diff(second, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)", "31622400")

    @Test
    fun dateDiffWithBindings() = assertEval("date_diff(year, a, b)",
                                           "1",
                                           mapOf("a" to "2016-01-10T05:30:55Z",
                                                 "b" to "2017-01-10T05:30:55Z").toSession())

    @Test
    fun wrongArgumentTypes1() = assertThrows("Expected text: 1", NodeMetadata(1, 1)) {
        voidEval("date_diff(1, `2016-01-10T05:30:55Z`, `2017-01-10T05:30:55Z`)")
    }

    @Test
    fun wrongArgumentTypes2() = assertThrows("Expected timestamp: 1", NodeMetadata(1, 1)) {
        voidEval("date_diff(second, 1, `2017-01-10T05:30:55Z`)")
    }

    @Test
    fun wrongArgumentTypes3() = assertThrows("Expected timestamp: 1", NodeMetadata(1, 1)) {
        voidEval("date_diff(second, `2017-01-10T05:30:55Z`, 1)")
    }
}