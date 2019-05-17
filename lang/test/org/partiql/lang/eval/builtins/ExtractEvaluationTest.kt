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

import org.partiql.lang.eval.*
import org.junit.*

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
    fun wrongArgumentTypes() = assertThrows("Expected timestamp: 1", NodeMetadata(1, 1)) {
        voidEval("extract(year from 1)")
    }
}