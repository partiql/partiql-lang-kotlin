package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.toSession
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.types.StaticType

class DateAddEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(DateAddPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(testCase.source, testCase.session, testCase.expectedLegacyModeResult, expectedPermissiveModeResult = testCase.expectedPermissiveModeResult)

    class DateAddPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExprFunctionTestCase("date_add(second, null, `2017-01-10T05:30:55Z`)", "null"),
            ExprFunctionTestCase("date_add(second, 1, null)", "null"),
            ExprFunctionTestCase("date_add(second, missing, `2017-01-10T05:30:55Z`)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("date_add(second, 1, missing)", "null", "$MISSING_ANNOTATION::null"),
            ExprFunctionTestCase("date_add(year, `1`, `2017T`)", "2018T"),
            ExprFunctionTestCase(
                "date_add(second, a, b)",
                "2017-01-10T05:30:56Z",
                session = mapOf("a" to "1", "b" to "2017-01-10T05:30:55Z").toSession()
            ),

            // add 1 at different precision levels
            ExprFunctionTestCase("date_add(year, 1, `2017T`)", "2018T"),
            ExprFunctionTestCase("date_add(month, 1, `2017T`)", "2017-02T"),
            ExprFunctionTestCase("date_add(day, 1, `2017T`)", "2017-01-02T"),
            ExprFunctionTestCase("date_add(hour, 1, `2017T`)", "2017-01-01T01:00-00:00"),
            ExprFunctionTestCase("date_add(minute, 1, `2017T`)", "2017-01-01T00:01-00:00"),
            ExprFunctionTestCase("date_add(second, 1, `2017T`)", "2017-01-01T00:00:01-00:00"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01T`)", "2018-01T"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01T`)", "2017-02T"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01T`)", "2017-01-02T"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01T`)", "2017-01-01T01:00-00:00"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01T`)", "2017-01-01T00:01-00:00"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01T`)", "2017-01-01T00:00:01-00:00"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T`)", "2018-01-02T"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T`)", "2017-02-02T"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T`)", "2017-01-03T"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T`)", "2017-01-02T01:00-00:00"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T`)", "2017-01-02T00:01-00:00"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T`)", "2017-01-02T00:00:01-00:00"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T03:04Z`)", "2018-01-02T03:04Z"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T03:04Z`)", "2017-02-02T03:04Z"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T03:04Z`)", "2017-01-03T03:04Z"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T03:04Z`)", "2017-01-02T04:04Z"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T03:04Z`)", "2017-01-02T03:05Z"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T03:04Z`)", "2017-01-02T03:04:01Z"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T03:04:05Z`)", "2018-01-02T03:04:05Z"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T03:04:05Z`)", "2017-02-02T03:04:05Z"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T03:04:05Z`)", "2017-01-03T03:04:05Z"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T03:04:05Z`)", "2017-01-02T04:04:05Z"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T03:04:05Z`)", "2017-01-02T03:05:05Z"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T03:04:05Z`)", "2017-01-02T03:04:06Z"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T03:04:05.006Z`)", "2018-01-02T03:04:05.006Z"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T03:04:05.006Z`)", "2017-02-02T03:04:05.006Z"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T03:04:05.006Z`)", "2017-01-03T03:04:05.006Z"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T03:04:05.006Z`)", "2017-01-02T04:04:05.006Z"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T03:04:05.006Z`)", "2017-01-02T03:05:05.006Z"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T03:04:05.006Z`)", "2017-01-02T03:04:06.006Z"),

            // add enough to flip a year. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(month, 12, `2017T`)", "2018-01T"),
            ExprFunctionTestCase("date_add(day, 365, `2017T`)", "2018-01-01T"),
            ExprFunctionTestCase("date_add(hour, ${365 * 24}, `2017T`)", "2018-01-01T00:00-00:00"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60}, `2017T`)", "2018-01-01T00:00-00:00"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60 * 60}, `2017T`)", "2076-12-17T00:00-00:00"),

            // add enough to flip a month. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(day, 31, `2017-01T`)", "2017-02-01T"),
            ExprFunctionTestCase("date_add(hour, ${31 * 24}, `2017-01T`)", "2017-02-01T00:00-00:00"),
            ExprFunctionTestCase("date_add(minute, ${31 * 24 * 60}, `2017-01T`)", "2017-02-01T00:00-00:00"),
            ExprFunctionTestCase("date_add(second, ${31 * 24 * 60 * 60}, `2017-01T`)", "2017-02-01T00:00:00-00:00"),

            // add enough to flip a day
            ExprFunctionTestCase("date_add(hour, 24, `2017-02-03T`)", "2017-02-04T00:00-00:00"),
            ExprFunctionTestCase("date_add(minute, ${24 * 60}, `2017-02-03T`)", "2017-02-04T00:00-00:00"),
            ExprFunctionTestCase("date_add(second, ${24 * 60 * 60}, `2017-02-03T`)", "2017-02-04T00:00:00-00:00"),

            // add enough to flip the hour
            ExprFunctionTestCase("date_add(minute, 60, `2017-02-04T05:06Z`)", "2017-02-04T06:06Z"),
            ExprFunctionTestCase("date_add(second, ${60 * 60}, `2017-02-04T05:06Z`)", "2017-02-04T06:06:00Z"),

            // add enough to flip the minute
            ExprFunctionTestCase("date_add(second, 60, `2017-02-04T05:06Z`)", "2017-02-04T05:07:00Z"),

            // subtract 1 at different precision levels
            ExprFunctionTestCase("date_add(year, -1, `2017T`)", "2016T"),
            ExprFunctionTestCase("date_add(month, -1, `2017T`)", "2016-12T"),
            ExprFunctionTestCase("date_add(day, -1, `2017T`)", "2016-12-31T"),
            ExprFunctionTestCase("date_add(hour, -1, `2017T`)", "2016-12-31T23:00-00:00"),
            ExprFunctionTestCase("date_add(minute, -1, `2017T`)", "2016-12-31T23:59-00:00"),
            ExprFunctionTestCase("date_add(second, -1, `2017T`)", "2016-12-31T23:59:59-00:00"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02T`)", "2016-02T"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02T`)", "2017-01T"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02T`)", "2017-01-31T"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02T`)", "2017-01-31T23:00-00:00"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02T`)", "2017-01-31T23:59-00:00"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02T`)", "2017-01-31T23:59:59-00:00"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T`)", "2016-02-03T"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T`)", "2017-01-03T"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T`)", "2017-02-02T"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T`)", "2017-02-02T23:00-00:00"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T`)", "2017-02-02T23:59-00:00"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T`)", "2017-02-02T23:59:59-00:00"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T04:05Z`)", "2016-02-03T04:05Z"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T04:05Z`)", "2017-01-03T04:05Z"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T04:05Z`)", "2017-02-02T04:05Z"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T04:05Z`)", "2017-02-03T03:05Z"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T04:05Z`)", "2017-02-03T04:04Z"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T04:05Z`)", "2017-02-03T04:04:59Z"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T04:05:06Z`)", "2016-02-03T04:05:06Z"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T04:05:06Z`)", "2017-01-03T04:05:06Z"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T04:05:06Z`)", "2017-02-02T04:05:06Z"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T04:05:06Z`)", "2017-02-03T03:05:06Z"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T04:05:06Z`)", "2017-02-03T04:04:06Z"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T04:05:06Z`)", "2017-02-03T04:05:05Z"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T04:05:06.007Z`)", "2016-02-03T04:05:06.007Z"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T04:05:06.007Z`)", "2017-01-03T04:05:06.007Z"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T04:05:06.007Z`)", "2017-02-02T04:05:06.007Z"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T04:05:06.007Z`)", "2017-02-03T03:05:06.007Z"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T04:05:06.007Z`)", "2017-02-03T04:04:06.007Z"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T04:05:06.007Z`)", "2017-02-03T04:05:05.007Z")
        )
    }

    // Error test cases: Invalid arguments
    data class InvalidArgTestCase(
        val query: String,
        val message: String
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgCases::class)
    fun dateAddInvalidArgumentTests(testCase: InvalidArgTestCase) =
        runEvaluatorErrorTestCase(
            testCase.query,
            ErrorCode.EVALUATOR_TIMESTAMP_OUT_OF_BOUNDS,
            expectedErrorContext = propertyValueMapOf(1, 1),
            expectedPermissiveModeResult = "MISSING"
        )

    class InvalidArgCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            InvalidArgTestCase(
                "date_add(year, 10000, `2017-06-27T`)",
                "Year 12017 must be between 1 and 9999 inclusive"
            ),
            InvalidArgTestCase(
                "date_add(year, -10000, `2000-06-27T`)",
                "Year -8001 must be between 1 and 9999 inclusive"
            ),
            InvalidArgTestCase(
                "date_add(month, 10000*12, `2017-06-27T`)",
                "Year 12017 must be between 1 and 9999 inclusive"
            ),
            InvalidArgTestCase(
                "date_add(month, -10000*12, `2000-06-27T`)",
                "Year -8001 must be between 1 and 9999 inclusive"
            )
        )
    }

    // Error test cases: Invalid argument type
    @Test
    fun dateAddInvalidArgTypeTest() = checkInvalidArgType(
        funcName = "date_add",
        syntaxSuffix = "(year,",
        args = listOf(
            Argument(2, StaticType.INT, ","),
            Argument(3, StaticType.TIMESTAMP, ")")
        )
    )

    // The invalid arity check is considered as syntax error and already done in the ParserErrorsTest.kt
}
