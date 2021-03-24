/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.builtins

import junitparams.Parameters
import org.partiql.lang.eval.*
import org.junit.*
import org.partiql.lang.syntax.DatePart
import java.time.LocalDate

/**
 * More detailed tests are in [ExtractExprFunctionTest] and parsing related tests in
 * [org.partiql.lang.syntax.SqlParserTest] and [org.partiql.lang.errors.ParserErrorsTest].
 */
class ExtractEvaluationTest : EvaluatorTestBase() {

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
    fun wrongArgumentTypes() = assertThrows("Expected date or timestamp: 1", NodeMetadata(1, 1)) {
        voidEval("extract(year from 1)")
    }

    data class ExtractFromDateTC(val source: String, val expected: ExprValue?)

    private fun createDateTC(source: String, expected: LocalDate) =
        DatePart.values()
            .map { datePart ->
                ExtractFromDateTC(
                    source = "EXTRACT(${datePart.name} FROM $source)",
                    expected = when (datePart) {
                        DatePart.YEAR -> valueFactory.newInt(expected.year)
                        DatePart.MONTH -> valueFactory.newInt(expected.monthValue)
                        DatePart.DAY -> valueFactory.newInt(expected.dayOfMonth)
                        DatePart.HOUR -> valueFactory.newInt(0)
                        DatePart.MINUTE -> valueFactory.newInt(0)
                        DatePart.SECOND -> valueFactory.newInt(0)
                        DatePart.TIMEZONE_HOUR -> null
                        DatePart.TIMEZONE_MINUTE -> null
                    }
                )
            }

    fun parametersForRunTests() = listOf(
        createDateTC("DATE '2012-12-12'", LocalDate.of(2012, 12, 12)),
        createDateTC("DATE '2020-02-29'", LocalDate.of(2020, 2, 29)),
        createDateTC("DATE '2021-03-24'", LocalDate.of(2021, 3, 24))
    ).flatten()

    @Test
    @Parameters
    fun runTests(tc: ExtractFromDateTC) = when (tc.expected) {
        null -> {
            try {
                voidEval(tc.source)
                fail("Expected evaluation error")
            } catch (e: EvaluationException) {
                // Do nothing
            }
        }
        else -> assertExprEquals(eval(tc.source), tc.expected)
    }
}