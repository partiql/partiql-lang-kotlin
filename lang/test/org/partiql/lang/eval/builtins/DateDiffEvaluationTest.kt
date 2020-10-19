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
 * More detailed tests are in [DateDiffExprFunctionTest] and parsing related tests in
 * [org.partiql.lang.syntax.SqlParserTest] and [org.partiql.lang.errors.ParserErrorsTest].
 */
class DateDiffEvaluationTest : EvaluatorTestBase() {

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
    fun dateDiffNull01() = assertEval("date_diff(second, null, `2017-01-10T05:30:55Z`)", "null")

    @Test
    fun dateDiffNull02() = assertEval("date_diff(second, `2016-01-10T05:30:55Z`, null)", "null")

    @Test
    fun dateDiffMissing01() = assertEval("date_diff(second, missing, `2017-01-10T05:30:55Z`)", "null")

    @Test
    fun dateDiffMissing02() = assertEval("date_diff(second, `2016-01-10T05:30:55Z`, missing)", "null")

    @Test
    fun dateDiffWithBindings() = assertEval("date_diff(year, a, b)",
                                           "1",
                                           mapOf("a" to "2016-01-10T05:30:55Z",
                                                 "b" to "2017-01-10T05:30:55Z").toSession())
    @Test
    fun wrongArgumentTypes2() = assertThrows("Expected timestamp: 1", NodeMetadata(1, 1)) {
        voidEval("date_diff(second, 1, `2017-01-10T05:30:55Z`)")
    }

    @Test
    fun wrongArgumentTypes3() = assertThrows("Expected timestamp: 1", NodeMetadata(1, 1)) {
        voidEval("date_diff(second, `2017-01-10T05:30:55Z`, 1)")
    }
}