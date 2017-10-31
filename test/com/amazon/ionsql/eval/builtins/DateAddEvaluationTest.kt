package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.eval.*
import org.junit.*

/**
 * More detailed tests are in [DateAddExprFunctionTest] and parsing related tests in [IonSqlParserTest] and
 * [ParserErrorsTest]
 */
class DateAddEvaluationTest : EvaluatorBase() {

    @Test
    fun dateAddYear() = assertEval("date_add(year, 1, `2017-01-10T05:30:55Z`)", "2018-01-10T05:30:55Z")

    @Test
    fun dateAddMonth() = assertEval("date_add(month, 1, `2017-01-10T05:30:55Z`)", "2017-02-10T05:30:55Z")

    @Test
    fun dateAddDay() = assertEval("date_add(day, 1, `2017-01-10T05:30:55Z`)", "2017-01-11T05:30:55Z")

    @Test
    fun dateAddHour() = assertEval("date_add(hour, 1, `2017-01-10T05:30:55Z`)", "2017-01-10T06:30:55Z")

    @Test
    fun dateAddMinute() = assertEval("date_add(minute, 1, `2017-01-10T05:30:55Z`)", "2017-01-10T05:31:55Z")

    @Test
    fun dateAddSecond() = assertEval("date_add(second, 1, `2017-01-10T05:30:55Z`)", "2017-01-10T05:30:56Z")

    @Test
    fun dateAddWithBindings() = assertEval("date_add(second, a, b)", "2017-01-10T05:30:56Z", mapOf(
        "a" to "1",
        "b" to "2017-01-10T05:30:55Z").toSession())

    @Test
    fun lessArguments() = assertThrows("date_add takes 3 arguments, received: 2", NodeMetadata(1, 1)) {
        voidEval("date_add(year, 1)")
    }

    @Test
    fun moreArguments() = assertThrows("date_add takes 3 arguments, received: 4", NodeMetadata(1, 1)) {
        voidEval("date_add(year, 1, `2017T`, 1)")
    }

    @Test
    fun wrongArgumentTypes() = assertThrows("No such binding: foobar", NodeMetadata(1, 16)) {
        voidEval("date_add(year, \"foobar\", 1)")
    }
}