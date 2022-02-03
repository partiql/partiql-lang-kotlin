package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.errors.UNBOUND_QUOTED_IDENTIFIER_HINT
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.NodeMetadata

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
    fun wrongArgumentTypes() = assertThrows(
        "date_add(year, \"foobar\", 1)",
        "No such binding: foobar. $UNBOUND_QUOTED_IDENTIFIER_HINT",
        NodeMetadata(1, 16),
        "MISSING")

    @Test
    fun addingYearOutsideOfTimestampBoundaries() = assertThrows(
        "date_add(year, 10000, `2017-06-27T`)",
        "Year 12017 must be between 1 and 9999 inclusive",
        NodeMetadata(1, 1),
        "MISSING")

    @Test
    fun addingNegativeYearOutsideOfTimestampBoundaries() = assertThrows(
        "date_add(year, -10000, `2000-06-27T`)",
        "Year -8001 must be between 1 and 9999 inclusive",
        NodeMetadata(1, 1),
        "MISSING")

    @Test
    fun addingMonthsOutsideOfTimestampBoundaries() = assertThrows(
        "date_add(month, 10000*12, `2017-06-27T`)",
        "Year 12017 must be between 1 and 9999 inclusive",
        NodeMetadata(1, 1),
        "MISSING")

    @Test
    fun addingNegativeMonthsOutsideOfTimestampBoundaries() = assertThrows(
        "date_add(month, -10000*12, `2000-06-27T`)",
        "Year -8001 must be between 1 and 9999 inclusive",
        NodeMetadata(1, 1),
        "MISSING")
}
