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
 * More detailed tests are in [DateAddExprFunctionTest] and parsing related tests in
 * [org.partiql.lang.syntax.SqlParserTest] and [org.partiql.lang.errors.ParserErrorsTest].
 */
class DateAddEvaluationTest : EvaluatorTestBase() {

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
    fun dateAddNull02() = assertEval("date_add(second, null, `2017-01-10T05:30:55Z`)", "null")

    @Test
    fun dateAddNull03() = assertEval("date_add(second, 1, null)", "null")

    @Test
    fun dateAddMissing02() = assertEval("date_add(second, missing, `2017-01-10T05:30:55Z`)", "null")

    @Test
    fun dateAddMissing03() = assertEval("date_add(second, 1, missing)", "null")

    @Test
    fun dateAddWithBindings() = assertEval("date_add(second, a, b)", "2017-01-10T05:30:56Z", mapOf(
        "a" to "1",
        "b" to "2017-01-10T05:30:55Z").toSession())

    @Test
    fun wrongArgumentTypes() = assertThrows("No such binding: foobar", NodeMetadata(1, 16)) {
        voidEval("date_add(year, \"foobar\", 1)")
    }

    @Test
    fun addingYearOutsideOfTimestampBoundaries() = assertThrows("Year 12017 must be between 1 and 9999 inclusive", NodeMetadata(1, 1)) {
        voidEval("date_add(year, 10000, `2017-06-27T`)")
    }

    @Test
    fun addingNegativeYearOutsideOfTimestampBoundaries() = assertThrows("Year -8001 must be between 1 and 9999 inclusive", NodeMetadata(1, 1)) {
        voidEval("date_add(year, -10000, `2000-06-27T`)")
    }

    @Test
    fun addingMonthsOutsideOfTimestampBoundaries() = assertThrows("Year 12017 must be between 1 and 9999 inclusive", NodeMetadata(1, 1)) {
        voidEval("date_add(month, 10000*12, `2017-06-27T`)")
    }

    @Test
    fun addingNegativeMonthsOutsideOfTimestampBoundaries() = assertThrows("Year -8001 must be between 1 and 9999 inclusive", NodeMetadata(1, 1)) {
        voidEval("date_add(month, -10000*12, `2000-06-27T`)")
    }
}
