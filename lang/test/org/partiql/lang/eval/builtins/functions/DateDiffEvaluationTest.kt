package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.toSession
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

class DateDiffEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(DateDiffPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(
            query = testCase.source,
            session = testCase.session,
            expectedResult = testCase.expectedLegacyModeResult,
            expectedPermissiveModeResult = testCase.expectedPermissiveModeResult
        )

    class DateDiffPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("date_diff(second, null, `2017-01-10T05:30:55Z`)", "NULL"),
            ExprFunctionTestCase("date_diff(second, `2016-01-10T05:30:55Z`, null)", "NULL"),
            ExprFunctionTestCase("date_diff(second, missing, `2017-01-10T05:30:55Z`)", "NULL", "MISSING"),
            ExprFunctionTestCase("date_diff(second, `2016-01-10T05:30:55Z`, missing)", "NULL", "MISSING"),
            ExprFunctionTestCase(
                "date_diff(year, a, b)",
                "1",
                session = mapOf("a" to "2016-01-10T05:30:55Z", "b" to "2017-01-10T05:30:55Z").toSession()
            ),

            // same dates
            ExprFunctionTestCase("date_diff(year, `2017T`, `2017T`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017T`, `2017T`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017T`, `2017T`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017T`, `2017T`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017T`, `2017T`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017T`, `2017T`)", "0"),

            ExprFunctionTestCase("date_diff(year, `2017-01T`, `2017-01T`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01T`, `2017-01T`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01T`, `2017-01T`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01T`, `2017-01T`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01T`, `2017-01T`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01T`, `2017-01T`)", "0"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T`, `2017-01-02T`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T`, `2017-01-02T`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T`, `2017-01-02T`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T`, `2017-01-02T`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T`, `2017-01-02T`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T`, `2017-01-02T`)", "0"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04Z`, `2017-01-02T03:04Z`)", "0"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:05Z`)", "0"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.007Z`)", "0"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04:05.007+08:09`, `2017-01-02T03:04:05.007+08:09`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04:05.007+08:09`, `2017-01-02T03:04:05.007+08:09`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04:05.007+08:09`, `2017-01-02T03:04:05.007+08:09`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04:05.007+08:09`, `2017-01-02T03:04:05.007+08:09`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04:05.007+08:09`, `2017-01-02T03:04:05.007+08:09`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04:05.007+08:09`, `2017-01-02T03:04:05.007+08:09`)", "0"),

            // later - earlier
            ExprFunctionTestCase("date_diff(year, `2017T`, `2018T`)", "1"),
            ExprFunctionTestCase("date_diff(month, `2017T`, `2018T`)", "12"),
            ExprFunctionTestCase("date_diff(day, `2017T`, `2018T`)", "365"),
            ExprFunctionTestCase("date_diff(hour, `2017T`, `2018T`)", "${365 * 24}"),
            ExprFunctionTestCase("date_diff(minute, `2017T`, `2018T`)", "${365 * 24 * 60}"),
            ExprFunctionTestCase("date_diff(second, `2017T`, `2018T`)", "${365 * 24 * 60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-01T`, `2017-02T`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01T`, `2017-02T`)", "1"),
            ExprFunctionTestCase("date_diff(day, `2017-01T`, `2017-02T`)", "31"),
            ExprFunctionTestCase("date_diff(hour, `2017-01T`, `2017-02T`)", "${31 * 24}"),
            ExprFunctionTestCase("date_diff(minute, `2017-01T`, `2017-02T`)", "${31 * 24 * 60}"),
            ExprFunctionTestCase("date_diff(second, `2017-01T`, `2017-02T`)", "${31 * 24 * 60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T`, `2017-01-03T`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T`, `2017-01-03T`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T`, `2017-01-03T`)", "1"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T`, `2017-01-03T`)", "24"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T`, `2017-01-03T`)", "${24 * 60}"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T`, `2017-01-03T`)", "${24 * 60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04Z`, `2017-01-02T04:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04Z`, `2017-01-02T04:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04Z`, `2017-01-02T04:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04Z`, `2017-01-02T04:04Z`)", "1"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04Z`, `2017-01-02T04:04Z`)", "60"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04Z`, `2017-01-02T04:04Z`)", "${60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04Z`, `2017-01-02T03:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04Z`, `2017-01-02T03:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04Z`, `2017-01-02T03:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04Z`, `2017-01-02T03:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04Z`, `2017-01-02T03:05Z`)", "1"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04Z`, `2017-01-02T03:05Z`)", "60"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:06Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:06Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:06Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:06Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:06Z`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04:05Z`, `2017-01-02T03:04:06Z`)", "1"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.008Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.008Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.008Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.008Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.008Z`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04:05.007Z`, `2017-01-02T03:04:05.008Z`)", "0"),

            // earlier - later
            ExprFunctionTestCase("date_diff(year, `2018T`, `2017T`)", "-1"),
            ExprFunctionTestCase("date_diff(month, `2018T`, `2017T`)", "-12"),
            ExprFunctionTestCase("date_diff(day, `2018T`, `2017T`)", "-365"),
            ExprFunctionTestCase("date_diff(hour, `2018T`, `2017T`)", "${-365 * 24}"),
            ExprFunctionTestCase("date_diff(minute, `2018T`, `2017T`)", "${-365 * 24 * 60}"),
            ExprFunctionTestCase("date_diff(second, `2018T`, `2017T`)", "${-365 * 24 * 60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-02T`, `2017-01T`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-02T`, `2017-01T`)", "-1"),
            ExprFunctionTestCase("date_diff(day, `2017-02T`, `2017-01T`)", "-31"),
            ExprFunctionTestCase("date_diff(hour, `2017-02T`, `2017-01T`)", "${-31 * 24}"),
            ExprFunctionTestCase("date_diff(minute, `2017-02T`, `2017-01T`)", "${-31 * 24 * 60}"),
            ExprFunctionTestCase("date_diff(second, `2017-02T`, `2017-01T`)", "${-31 * 24 * 60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-01-03T`, `2017-01-02T`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-03T`, `2017-01-02T`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-03T`, `2017-01-02T`)", "-1"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-03T`, `2017-01-02T`)", "-24"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-03T`, `2017-01-02T`)", "${-24 * 60}"),
            ExprFunctionTestCase("date_diff(second, `2017-01-03T`, `2017-01-02T`)", "${-24 * 60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T04:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T04:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T04:04Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T04:04Z`, `2017-01-02T03:04Z`)", "-1"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T04:04Z`, `2017-01-02T03:04Z`)", "-60"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T04:04Z`, `2017-01-02T03:04Z`)", "${-60 * 60}"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:05Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:05Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:05Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:05Z`, `2017-01-02T03:04Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:05Z`, `2017-01-02T03:04Z`)", "-1"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:05Z`, `2017-01-02T03:04Z`)", "-60"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04:06Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04:06Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04:06Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04:06Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04:06Z`, `2017-01-02T03:04:05Z`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04:06Z`, `2017-01-02T03:04:05Z`)", "-1"),

            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04:05.008Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04:05.008Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T03:04:05.008Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04:05.008Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04:05.008Z`, `2017-01-02T03:04:05.007Z`)", "0"),
            ExprFunctionTestCase("date_diff(second, `2017-01-02T03:04:05.008Z`, `2017-01-02T03:04:05.007Z`)", "0"),

            // on different local offsets
            ExprFunctionTestCase("date_diff(year, `2017-01-02T03:04+01:02`, `2017-01-02T03:04+00:00`)", "0"),
            ExprFunctionTestCase("date_diff(month, `2017-01-02T03:04+00:02`, `2017-01-02T03:04+00:00`)", "0"),
            ExprFunctionTestCase("date_diff(day, `2017-01-02T01:00+10:00`, `2017-01-02T01:00+00:00`)", "0"),
            ExprFunctionTestCase("date_diff(hour, `2017-01-02T03:04+01:02`, `2017-01-02T03:04+00:00`)", "1"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-02T03:04+00:02`, `2017-01-02T03:04+00:00`)", "2"),

            // different precisions
            // year
            ExprFunctionTestCase("date_diff(month, `2017T`, `2017-02T`)", "1"),
            ExprFunctionTestCase("date_diff(day, `2017T`, `2017-01-02T`)", "1"),
            ExprFunctionTestCase("date_diff(hour, `2017T`, `2017-01-01T01:00Z`)", "1"),
            ExprFunctionTestCase("date_diff(minute, `2017T`, `2017-01-01T00:01Z`)", "1"),
            ExprFunctionTestCase("date_diff(second, `2017T`, `2017-01-01T00:00:01Z`)", "1"),

            // month
            ExprFunctionTestCase("date_diff(day, `2017-01T`, `2017-01-02T`)", "1"),
            ExprFunctionTestCase("date_diff(hour, `2017-01T`, `2017-01-01T01:00Z`)", "1"),
            ExprFunctionTestCase("date_diff(minute, `2017-01T`, `2017-01-01T00:01Z`)", "1"),
            ExprFunctionTestCase("date_diff(second, `2017-01T`, `2017-01-01T00:00:01Z`)", "1"),

            // day
            ExprFunctionTestCase("date_diff(hour, `2017-01-01T`, `2017-01-01T01:00Z`)", "1"),
            ExprFunctionTestCase("date_diff(minute, `2017-01-01T`, `2017-01-01T00:01Z`)", "1"),
            ExprFunctionTestCase("date_diff(second, `2017-01-01T`, `2017-01-01T00:00:01Z`)", "1"),

            // minute
            ExprFunctionTestCase("date_diff(second, `2017-01-01T00:00Z`, `2017-01-01T00:00:01Z`)", "1"),

            // leap year
            ExprFunctionTestCase("date_diff(day, `2016-01-01T`, `2017-01-01T`)", "366"),
            ExprFunctionTestCase("date_diff(hour, `2016-01-01T`, `2017-01-01T`)", "${366 * 24}"),
            ExprFunctionTestCase("date_diff(minute, `2016-01-01T`, `2017-01-01T`)", "${366 * 24 * 60}"),
            ExprFunctionTestCase("date_diff(second, `2016-01-01T`, `2017-01-01T`)", "${366 * 24 * 60 * 60}"),

            // Days in a month
            ExprFunctionTestCase("date_diff(day, `2017-01-01T`, `2017-02-01T`)", "31"), // January
            ExprFunctionTestCase("date_diff(day, `2017-02-01T`, `2017-03-01T`)", "28"), // February
            ExprFunctionTestCase("date_diff(day, `2016-02-01T`, `2016-03-01T`)", "29"), // February leap year
            ExprFunctionTestCase("date_diff(day, `2017-03-01T`, `2017-04-01T`)", "31"), // March
            ExprFunctionTestCase("date_diff(day, `2017-04-01T`, `2017-05-01T`)", "30"), // April
            ExprFunctionTestCase("date_diff(day, `2017-05-01T`, `2017-06-01T`)", "31"), // May
            ExprFunctionTestCase("date_diff(day, `2017-06-01T`, `2017-07-01T`)", "30"), // June
            ExprFunctionTestCase("date_diff(day, `2017-07-01T`, `2017-08-01T`)", "31"), // July
            ExprFunctionTestCase("date_diff(day, `2017-08-01T`, `2017-09-01T`)", "31"), // August
            ExprFunctionTestCase("date_diff(day, `2017-09-01T`, `2017-10-01T`)", "30"), // September
            ExprFunctionTestCase("date_diff(day, `2017-10-01T`, `2017-11-01T`)", "31"), // October
            ExprFunctionTestCase("date_diff(day, `2017-11-01T`, `2017-12-01T`)", "30"), // November
            ExprFunctionTestCase("date_diff(day, `2017-12-01T`, `2018-01-01T`)", "31") // December
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun dateDiffInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "date_diff",
        syntaxSuffix = "(day,",
        args = listOf(
            Argument(2, StaticType.TIMESTAMP, ","),
            Argument(3, StaticType.TIMESTAMP, ")")
        )
    )

    // The invalid arity check is considered as syntax error and already done in the ParserErrorsTest.kt
}
