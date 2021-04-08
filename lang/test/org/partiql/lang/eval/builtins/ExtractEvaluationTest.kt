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

import com.amazon.ion.Timestamp
import junitparams.Parameters
import org.assertj.core.api.Assertions
import org.partiql.lang.eval.*
import org.junit.*
import org.partiql.lang.syntax.DatePart
import java.time.LocalDate

/**
 * Parsing related tests in [org.partiql.lang.syntax.SqlParserTest] and [org.partiql.lang.errors.ParserErrorsTest].
 */
class ExtractEvaluationTest : EvaluatorTestBase() {

    private val env = Environment.standard()

    private val subject = ExtractExprFunction(valueFactory)

    @Test
    fun extractYear() = assertEval("extract(year from `2017-01-10T05:30:55Z`)", "2017e0")

    @Test
    fun extractMonth() = assertEval("extract(month from `2017-01-10T05:30:55Z`)", "1e0")

    @Test
    fun extractDay() = assertEval("extract(day from `2017-01-10T05:30:55Z`)", "10e0")

    @Test
    fun extractHour() = assertEval("extract(hour from `2017-01-10T05:30:55Z`)", "5e0")

    @Test
    fun extractMinute() = assertEval("extract(minute from `2017-01-10T05:30:55Z`)", "30e0")

    @Test
    fun extractSecond() = assertEval("extract(second from `2017-01-10T05:30:55Z`)", "55e0")

    @Test
    fun extractTimezoneHour() = assertEval("extract(timezone_hour from `2017-01-10T05:30:55+11:30`)", "11e0")

    @Test
    fun extractTimezoneMinute() = assertEval("extract(timezone_minute from `2017-01-10T05:30:55+11:30`)", "30e0")

    @Test
    fun extractFromNull() = assertEval("extract(timezone_minute from null)", "null")

    @Test
    fun extractFromMissing() = assertEval("extract(timezone_minute from missing)", "null")

    @Test
    fun extractTimezoneHourNegativeOffset() =
        assertEval("extract(timezone_hour from `2017-01-10T05:30:55-11:30`)", "-11e0")

    @Test
    fun extractTimezoneMinuteNegativeOffset() =
        assertEval("extract(timezone_minute from `2017-01-10T05:30:55-11:30`)", "-30e0")

    @Test
    fun extractWithBindings() = assertEval("extract(second from a)",
                                           "55e0",
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

    private fun callExtract(vararg args: Any): Number? {
        val value = subject.call(env, args.map { anyToExprValue(it) }.toList())
        return when(value.type) {
            ExprValueType.NULL -> null
            else -> value.numberValue()
        }
    }

    @Test
    fun lessArguments() {
        Assertions.assertThatThrownBy { callExtract("year") }
            .hasMessage("extract takes exactly 2 arguments, received: 1")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test fun moreArguments() {
        Assertions.assertThatThrownBy { callExtract("year", 1, 1) }
            .hasMessage("extract takes exactly 2 arguments, received: 3")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfFirstArgument() {
        Assertions.assertThatThrownBy { callExtract("foobar", 1) }
            .hasMessage("invalid date part, valid values: [year, month, day, hour, minute, second, timezone_hour, timezone_minute]")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    @Test
    fun wrongTypeOfSecondArgument() {
        Assertions.assertThatThrownBy { callExtract("year", "999") }
            .hasMessage("Expected date or timestamp: '999'")
            .isExactlyInstanceOf(EvaluationException::class.java)
    }

    fun parametersForExtract(): List<Pair<Number?, () -> Number?>> = listOf(
        // just year
        2017 to { callExtract("year", Timestamp.valueOf("2017T")) },
        1 to { callExtract("month", Timestamp.valueOf("2017T")) },
        1 to { callExtract("day", Timestamp.valueOf("2017T")) },
        0 to { callExtract("hour", Timestamp.valueOf("2017T")) },
        0 to { callExtract("minute", Timestamp.valueOf("2017T")) },
        0 to { callExtract("second", Timestamp.valueOf("2017T")) },
        0 to { callExtract("timezone_hour", Timestamp.valueOf("2017T")) },
        0 to { callExtract("timezone_minute", Timestamp.valueOf("2017T")) },
        // year, month
        2017 to { callExtract("year", Timestamp.valueOf("2017-01T")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01T")) },
        1 to { callExtract("day", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("hour", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("minute", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("second", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("timezone_hour", Timestamp.valueOf("2017-01T")) },
        0 to { callExtract("timezone_minute", Timestamp.valueOf("2017-01T")) },

        // year, month, day
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("hour", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("minute", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("second", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T")) },
        0 to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T")) },

        // year, month, day, hour, minute
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T03:04Z")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T03:04Z")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T03:04Z")) },
        3    to { callExtract("hour", Timestamp.valueOf("2017-01-02T03:04Z")) },
        4    to { callExtract("minute", Timestamp.valueOf("2017-01-02T03:04Z")) },
        0    to { callExtract("second", Timestamp.valueOf("2017-01-02T03:04Z")) },
        0    to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04Z")) },
        0    to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04Z")) },

        // year, month, day, hour, minute, second
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        3    to { callExtract("hour", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        4    to { callExtract("minute", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        5    to { callExtract("second", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0    to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04:05Z")) },
        0    to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04:05Z")) },

        // year, month, day, hour, minute, second, local offset
        2017 to { callExtract("year", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        1    to { callExtract("month", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        2    to { callExtract("day", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        3    to { callExtract("hour", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        4    to { callExtract("minute", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        5    to { callExtract("second", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        7    to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },
        8    to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04:05+07:08")) },

        // negative offset
        -7 to { callExtract("timezone_hour", Timestamp.valueOf("2017-01-02T03:04:05-07:08")) },
        -8 to { callExtract("timezone_minute", Timestamp.valueOf("2017-01-02T03:04:05-07:08")) },

        // extract year, month, day, hour, minute, second from DATE literals
        2021 to { callExtract("year", LocalDate.of(2021, 3, 24)) },
        3 to { callExtract("month", LocalDate.of(2021, 3, 24)) },
        24 to { callExtract("day", LocalDate.of(2021, 3, 24)) },
        0 to { callExtract("hour", LocalDate.of(2021, 3, 24)) },
        0 to { callExtract("minute", LocalDate.of(2021, 3, 24)) },
        0 to { callExtract("second", LocalDate.of(2021, 3, 24)) }
    )

    @Test
    @Parameters
    fun extract(params: Pair<Number?, () -> Number?>) {
        val (expected, call) = params

        assertEquals(expected?.toLong(), call.invoke()?.toLong())
    }
}