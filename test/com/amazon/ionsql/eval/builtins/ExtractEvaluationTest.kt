package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.eval.*
import org.junit.*

/**
 * More detailed tests are in [ExtractExprFunctionTest] and parsing related tests in [IonSqlParserTest] and
 * [ParserErrorsTest]
 */
class ExtractEvaluationTest : EvaluatorBase() {

    @Test
    fun extractYear() = assertEval("extract(year from `2017-01-10T05:30:55Z`)", "2017")

    @Test
    fun extractMonth() = assertEval("extract(month from `2017-01-10T05:30:55Z`)", "1")

    @Test
    fun extractDay() = assertEval("extract(day from `2017-01-10T05:30:55Z`)", "10")

    @Test
    fun extractHour() = assertEval("extract(hour from `2017-01-10T05:30:55Z`)", "5")

    @Test
    fun extractMinute() = assertEval("extract(minute from `2017-01-10T05:30:55Z`)", "30")

    @Test
    fun extractSecond() = assertEval("extract(second from `2017-01-10T05:30:55Z`)", "55")

    @Test
    fun extractTimezoneHour() = assertEval("extract(timezone_hour from `2017-01-10T05:30:55+11:30`)", "11")

    @Test
    fun extractTimezoneMinute() = assertEval("extract(timezone_minute from `2017-01-10T05:30:55+11:30`)", "30")

    @Test
    fun extractFromNull() = assertEval("extract(timezone_minute from null)", "null")

    @Test
    fun extractFromMissing() = assertEval("extract(timezone_minute from missing)", "null")

    @Test
    fun extractTimezoneHourNegativeOffset() =
        assertEval("extract(timezone_hour from `2017-01-10T05:30:55-11:30`)", "-11")

    @Test
    fun extractTimezoneMinuteNegativeOffset() =
        assertEval("extract(timezone_minute from `2017-01-10T05:30:55-11:30`)", "-30")

    @Test
    fun extractWithBindings() = assertEval("extract(second from a)",
                                           "55",
                                           mapOf("a" to "2017-01-10T05:30:55Z").toSession())

    @Test
    fun wrongArgumentTypes() = assertThrows("Expected timestamp: 1", NodeMetadata(1, 1)) {
        voidEval("extract(year from 1)")
    }
}