package org.partiql.lang.eval.builtins.functions

import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.Argument
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.eval.builtins.checkInvalidArgType
import org.partiql.lang.eval.builtins.toSession
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.types.StaticType

class DateAddEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(DateAddPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) =
        runEvaluatorTestCase(
            query = testCase.source,
            session = testCase.session,
            expectedResult = testCase.expectedLegacyModeResult,
            expectedPermissiveModeResult = testCase.expectedPermissiveModeResult,
            expectedResultFormat = ExpectedResultFormat.STRICT
        )

    class DateAddPassCases : ArgumentsProviderBase() {
        override fun getParameters() = ionTestCases + partiQLTimestampWithTimeZone + partiQLTimestampWithoutTimeZone

        // All the following tests case has a ion literal for timestamp parameter in date add function
        private val ionTestCases = listOf(
            // Against Ion Timestamp
            ExprFunctionTestCase("date_add(second, null, `2017-01-10T05:30:55Z`)", "null"),
            ExprFunctionTestCase("date_add(second, 1, null)", "null"),
            ExprFunctionTestCase("date_add(second, missing, `2017-01-10T05:30:55Z`)", "null", "MISSING"),
            ExprFunctionTestCase("date_add(second, 1, missing)", "null", "MISSING"),
            ExprFunctionTestCase("date_add(year, `1`, `2017T`)", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase(
                "date_add(second, a, b)",
                "TIMESTAMP '2017-01-10T05:30:56+00:00'",
                session = mapOf("a" to "1", "b" to "2017-01-10T05:30:55Z").toSession()
            ),
            ExprFunctionTestCase("date_add(year, 1, `2017T`)", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, 1, `2017T`)", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 1, `2017T`)", "TIMESTAMP '2017-01-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, `2017T`)", "TIMESTAMP '2017-01-01T01:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, `2017T`)", "TIMESTAMP '2017-01-01T00:01:00-00:00'"),
            ExprFunctionTestCase("date_add(second, 1, `2017T`)", "TIMESTAMP '2017-01-01T00:00:01-00:00'"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01T`)", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01T`)", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01T`)", "TIMESTAMP '2017-01-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01T`)", "TIMESTAMP '2017-01-01T01:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01T`)", "TIMESTAMP '2017-01-01T00:01:00-00:00'"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01T`)", "TIMESTAMP '2017-01-01T00:00:01-00:00'"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T`)", "TIMESTAMP '2018-01-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T`)", "TIMESTAMP '2017-02-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T`)", "TIMESTAMP '2017-01-03T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T`)", "TIMESTAMP '2017-01-02T01:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T`)", "TIMESTAMP '2017-01-02T00:01:00-00:00'"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T`)", "TIMESTAMP '2017-01-02T00:00:01-00:00'"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T03:04Z`)", "TIMESTAMP '2018-01-02T03:04:00+00:00'"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T03:04Z`)", "TIMESTAMP '2017-02-02T03:04:00+00:00'"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T03:04Z`)", "TIMESTAMP '2017-01-03T03:04:00+00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T03:04Z`)", "TIMESTAMP '2017-01-02T04:04:00+00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T03:04Z`)", "TIMESTAMP '2017-01-02T03:05:00+00:00'"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T03:04Z`)", "TIMESTAMP '2017-01-02T03:04:01+00:00'"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T03:04:05Z`)", "TIMESTAMP '2018-01-02T03:04:05+00:00'"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T03:04:05Z`)", "TIMESTAMP '2017-02-02T03:04:05+00:00'"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T03:04:05Z`)", "TIMESTAMP '2017-01-03T03:04:05+00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T03:04:05Z`)", "TIMESTAMP '2017-01-02T04:04:05+00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T03:04:05Z`)", "TIMESTAMP '2017-01-02T03:05:05+00:00'"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T03:04:05Z`)", "TIMESTAMP '2017-01-02T03:04:06+00:00'"),

            ExprFunctionTestCase("date_add(year, 1, `2017-01-02T03:04:05.006Z`)", "TIMESTAMP '2018-01-02T03:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(month, 1, `2017-01-02T03:04:05.006Z`)", "TIMESTAMP '2017-02-02T03:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(day, 1, `2017-01-02T03:04:05.006Z`)", "TIMESTAMP '2017-01-03T03:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, `2017-01-02T03:04:05.006Z`)", "TIMESTAMP '2017-01-02T04:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, `2017-01-02T03:04:05.006Z`)", "TIMESTAMP '2017-01-02T03:05:05.006+00:00'"),
            ExprFunctionTestCase("date_add(second, 1, `2017-01-02T03:04:05.006Z`)", "TIMESTAMP '2017-01-02T03:04:06.006+00:00'"),

            // add enough to flip a year. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(month, 12, `2017T`)", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 365, `2017T`)", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, ${365 * 24}, `2017T`)", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60}, `2017T`)", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60 * 60}, `2017T`)", "TIMESTAMP '2076-12-17T00:00:00-00:00'"),

            // add enough to flip a month. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(day, 31, `2017-01T`)", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, ${31 * 24}, `2017-01T`)", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${31 * 24 * 60}, `2017-01T`)", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(second, ${31 * 24 * 60 * 60}, `2017-01T`)", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),

            // add enough to flip a day
            ExprFunctionTestCase("date_add(hour, 24, `2017-02-03T`)", "TIMESTAMP '2017-02-04T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${24 * 60}, `2017-02-03T`)", "TIMESTAMP '2017-02-04T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(second, ${24 * 60 * 60}, `2017-02-03T`)", "TIMESTAMP '2017-02-04T00:00:00-00:00'"),

            // add enough to flip the hour
            ExprFunctionTestCase("date_add(minute, 60, `2017-02-04T05:06Z`)", "TIMESTAMP '2017-02-04T06:06:00+00:00'"),
            ExprFunctionTestCase("date_add(second, ${60 * 60}, `2017-02-04T05:06Z`)", "TIMESTAMP '2017-02-04T06:06:00+00:00'"),

            // add enough to flip the minute
            ExprFunctionTestCase("date_add(second, 60, `2017-02-04T05:06Z`)", "TIMESTAMP '2017-02-04T05:07:00+00:00'"),

            // subtract 1 at different precision levels
            ExprFunctionTestCase("date_add(year, -1, `2017T`)", "TIMESTAMP '2016-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, -1, `2017T`)", "TIMESTAMP '2016-12-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, -1, `2017T`)", "TIMESTAMP '2016-12-31T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, `2017T`)", "TIMESTAMP '2016-12-31T23:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, `2017T`)", "TIMESTAMP '2016-12-31T23:59:00-00:00'"),
            ExprFunctionTestCase("date_add(second, -1, `2017T`)", "TIMESTAMP '2016-12-31T23:59:59-00:00'"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02T`)", "TIMESTAMP '2016-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02T`)", "TIMESTAMP '2017-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02T`)", "TIMESTAMP '2017-01-31T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02T`)", "TIMESTAMP '2017-01-31T23:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02T`)", "TIMESTAMP '2017-01-31T23:59:00-00:00'"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02T`)", "TIMESTAMP '2017-01-31T23:59:59-00:00'"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T`)", "TIMESTAMP '2016-02-03T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T`)", "TIMESTAMP '2017-01-03T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T`)", "TIMESTAMP '2017-02-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T`)", "TIMESTAMP '2017-02-02T23:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T`)", "TIMESTAMP '2017-02-02T23:59:00-00:00'"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T`)", "TIMESTAMP '2017-02-02T23:59:59-00:00'"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T04:05Z`)", "TIMESTAMP '2016-02-03T04:05:00+00:00'"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T04:05Z`)", "TIMESTAMP '2017-01-03T04:05:00+00:00'"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T04:05Z`)", "TIMESTAMP '2017-02-02T04:05:00+00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T04:05Z`)", "TIMESTAMP '2017-02-03T03:05:00+00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T04:05Z`)", "TIMESTAMP '2017-02-03T04:04:00+00:00'"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T04:05Z`)", "TIMESTAMP '2017-02-03T04:04:59+00:00'"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T04:05:06Z`)", "TIMESTAMP '2016-02-03T04:05:06+00:00'"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T04:05:06Z`)", "TIMESTAMP '2017-01-03T04:05:06+00:00'"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T04:05:06Z`)", "TIMESTAMP '2017-02-02T04:05:06+00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T04:05:06Z`)", "TIMESTAMP '2017-02-03T03:05:06+00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T04:05:06Z`)", "TIMESTAMP '2017-02-03T04:04:06+00:00'"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T04:05:06Z`)", "TIMESTAMP '2017-02-03T04:05:05+00:00'"),

            ExprFunctionTestCase("date_add(year, -1, `2017-02-03T04:05:06.007Z`)", "TIMESTAMP '2016-02-03T04:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(month, -1, `2017-02-03T04:05:06.007Z`)", "TIMESTAMP '2017-01-03T04:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(day, -1, `2017-02-03T04:05:06.007Z`)", "TIMESTAMP '2017-02-02T04:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, `2017-02-03T04:05:06.007Z`)", "TIMESTAMP '2017-02-03T03:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, `2017-02-03T04:05:06.007Z`)", "TIMESTAMP '2017-02-03T04:04:06.007+00:00'"),
            ExprFunctionTestCase("date_add(second, -1, `2017-02-03T04:05:06.007Z`)", "TIMESTAMP '2017-02-03T04:05:05.007+00:00'")
        )

        private val partiQLTimestampWithTimeZone = listOf(
            ExprFunctionTestCase("date_add(second, null, TIMESTAMP '2017-01-10T05:30:55Z')", "null"),
            ExprFunctionTestCase("date_add(second, 1, null)", "null"),
            ExprFunctionTestCase("date_add(second, missing, TIMESTAMP '2017-01-10T05:30:55Z')", "null", "MISSING"),
            ExprFunctionTestCase("date_add(second, 1, missing)", "null", "MISSING"),
            ExprFunctionTestCase("date_add(year, `1`, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase(
                "date_add(second, a, b)",
                "TIMESTAMP '2017-01-10T05:30:56+00:00'",
                session = mapOf("a" to "1", "b" to "2017-01-10T05:30:55Z").toSession()
            ),
            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-01T01:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-01T00:01:00-00:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-01T00:00:01-00:00'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-01T01:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-01T00:01:00-00:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-01-01T00:00:01-00:00'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T00:00:00-00:00')", "TIMESTAMP '2018-01-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T00:00:00-00:00')", "TIMESTAMP '2017-02-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T00:00:00-00:00')", "TIMESTAMP '2017-01-03T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T00:00:00-00:00')", "TIMESTAMP '2017-01-02T01:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T00:00:00-00:00')", "TIMESTAMP '2017-01-02T00:01:00-00:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T00:00:00-00:00')", "TIMESTAMP '2017-01-02T00:00:01-00:00'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T03:04:00Z')", "TIMESTAMP '2018-01-02T03:04:00+00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T03:04:00Z')", "TIMESTAMP '2017-02-02T03:04:00+00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T03:04:00Z')", "TIMESTAMP '2017-01-03T03:04:00+00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T03:04:00Z')", "TIMESTAMP '2017-01-02T04:04:00+00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T03:04:00Z')", "TIMESTAMP '2017-01-02T03:05:00+00:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T03:04:00Z')", "TIMESTAMP '2017-01-02T03:04:01+00:00'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T03:04:05Z')", "TIMESTAMP '2018-01-02T03:04:05+00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T03:04:05Z')", "TIMESTAMP '2017-02-02T03:04:05+00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T03:04:05Z')", "TIMESTAMP '2017-01-03T03:04:05+00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T03:04:05Z')", "TIMESTAMP '2017-01-02T04:04:05+00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T03:04:05Z')", "TIMESTAMP '2017-01-02T03:05:05+00:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T03:04:05Z')", "TIMESTAMP '2017-01-02T03:04:06+00:00'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T03:04:05.006Z')", "TIMESTAMP '2018-01-02T03:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T03:04:05.006Z')", "TIMESTAMP '2017-02-02T03:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T03:04:05.006Z')", "TIMESTAMP '2017-01-03T03:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T03:04:05.006Z')", "TIMESTAMP '2017-01-02T04:04:05.006+00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T03:04:05.006Z')", "TIMESTAMP '2017-01-02T03:05:05.006+00:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T03:04:05.006Z')", "TIMESTAMP '2017-01-02T03:04:06.006+00:00'"),

            // add enough to flip a year. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(month, 12, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, 365, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, ${365 * 24}, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60}, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2018-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60 * 60}, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2076-12-17T00:00:00-00:00'"),

            // add enough to flip a month. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(day, 31, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, ${31 * 24}, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${31 * 24 * 60}, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(second, ${31 * 24 * 60 * 60}, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2017-02-01T00:00:00-00:00'"),

            // add enough to flip a day
            ExprFunctionTestCase("date_add(hour, 24, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-02-04T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, ${24 * 60}, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-02-04T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(second, ${24 * 60 * 60}, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-02-04T00:00:00-00:00'"),

            // add enough to flip the hour
            ExprFunctionTestCase("date_add(minute, 60, TIMESTAMP '2017-02-04T05:06:00Z')", "TIMESTAMP '2017-02-04T06:06:00+00:00'"),
            ExprFunctionTestCase("date_add(second, ${60 * 60}, TIMESTAMP '2017-02-04T05:06:00Z')", "TIMESTAMP '2017-02-04T06:06:00+00:00'"),

            // add enough to flip the minute
            ExprFunctionTestCase("date_add(second, 60, TIMESTAMP '2017-02-04T05:06:00Z')", "TIMESTAMP '2017-02-04T05:07:00+00:00'"),

            // subtract 1 at different precision levels
            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2016-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2016-12-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2016-12-31T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2016-12-31T23:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2016-12-31T23:59:00-00:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-01-01T00:00:00-00:00')", "TIMESTAMP '2016-12-31T23:59:59-00:00'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-01T00:00:00-00:00')", "TIMESTAMP '2016-02-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-01T00:00:00-00:00')", "TIMESTAMP '2017-01-01T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-01T00:00:00-00:00')", "TIMESTAMP '2017-01-31T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-01T00:00:00-00:00')", "TIMESTAMP '2017-01-31T23:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-01T00:00:00-00:00')", "TIMESTAMP '2017-01-31T23:59:00-00:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-01T00:00:00-00:00')", "TIMESTAMP '2017-01-31T23:59:59-00:00'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2016-02-03T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-01-03T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-02-02T00:00:00-00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-02-02T23:00:00-00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-02-02T23:59:00-00:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T00:00:00-00:00')", "TIMESTAMP '2017-02-02T23:59:59-00:00'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T04:05:00Z')", "TIMESTAMP '2016-02-03T04:05:00+00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T04:05:00Z')", "TIMESTAMP '2017-01-03T04:05:00+00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T04:05:00Z')", "TIMESTAMP '2017-02-02T04:05:00+00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T04:05:00Z')", "TIMESTAMP '2017-02-03T03:05:00+00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T04:05:00Z')", "TIMESTAMP '2017-02-03T04:04:00+00:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T04:05:00Z')", "TIMESTAMP '2017-02-03T04:04:59+00:00'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T04:05:06Z')", "TIMESTAMP '2016-02-03T04:05:06+00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T04:05:06Z')", "TIMESTAMP '2017-01-03T04:05:06+00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T04:05:06Z')", "TIMESTAMP '2017-02-02T04:05:06+00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T04:05:06Z')", "TIMESTAMP '2017-02-03T03:05:06+00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T04:05:06Z')", "TIMESTAMP '2017-02-03T04:04:06+00:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T04:05:06Z')", "TIMESTAMP '2017-02-03T04:05:05+00:00'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T04:05:06.007Z')", "TIMESTAMP '2016-02-03T04:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T04:05:06.007Z')", "TIMESTAMP '2017-01-03T04:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T04:05:06.007Z')", "TIMESTAMP '2017-02-02T04:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T04:05:06.007Z')", "TIMESTAMP '2017-02-03T03:05:06.007+00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T04:05:06.007Z')", "TIMESTAMP '2017-02-03T04:04:06.007+00:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T04:05:06.007Z')", "TIMESTAMP '2017-02-03T04:05:05.007+00:00'")
        )

        private val partiQLTimestampWithoutTimeZone = listOf(
            ExprFunctionTestCase("date_add(second, null, TIMESTAMP '2017-01-10T05:30:55')", "null"),
            ExprFunctionTestCase("date_add(second, 1, null)", "null"),
            ExprFunctionTestCase("date_add(second, missing, TIMESTAMP '2017-01-10T05:30:55')", "null", "MISSING"),
            ExprFunctionTestCase("date_add(second, 1, missing)", "null", "MISSING"),
            ExprFunctionTestCase("date_add(year, `1`, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2018-01-01T00:00:00'"),
            ExprFunctionTestCase(
                "date_add(second, a, b)",
                "TIMESTAMP '2017-01-10T05:30:56'",
                session = mapOf("a" to "1", "b" to "\$timestamp_without_timezone::{year: 2017, month: 1, day: 10, hour: 5, minute: 30, second: 55.}").toSession()
            ),
            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2018-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-02-01T00:00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-02T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-01T01:00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-01T00:01:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-01T00:00:01'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2018-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-02-01T00:00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-02T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-01T01:00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-01T00:01:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-01-01T00:00:01'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T00:00:00')", "TIMESTAMP '2018-01-02T00:00:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T00:00:00')", "TIMESTAMP '2017-02-02T00:00:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T00:00:00')", "TIMESTAMP '2017-01-03T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T00:00:00')", "TIMESTAMP '2017-01-02T01:00:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T00:00:00')", "TIMESTAMP '2017-01-02T00:01:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T00:00:00')", "TIMESTAMP '2017-01-02T00:00:01'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T03:04:00')", "TIMESTAMP '2018-01-02T03:04:00'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T03:04:00')", "TIMESTAMP '2017-02-02T03:04:00'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T03:04:00')", "TIMESTAMP '2017-01-03T03:04:00'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T03:04:00')", "TIMESTAMP '2017-01-02T04:04:00'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T03:04:00')", "TIMESTAMP '2017-01-02T03:05:00'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T03:04:00')", "TIMESTAMP '2017-01-02T03:04:01'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T03:04:05')", "TIMESTAMP '2018-01-02T03:04:05'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T03:04:05')", "TIMESTAMP '2017-02-02T03:04:05'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T03:04:05')", "TIMESTAMP '2017-01-03T03:04:05'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T03:04:05')", "TIMESTAMP '2017-01-02T04:04:05'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T03:04:05')", "TIMESTAMP '2017-01-02T03:05:05'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T03:04:05')", "TIMESTAMP '2017-01-02T03:04:06'"),

            ExprFunctionTestCase("date_add(year, 1, TIMESTAMP '2017-01-02T03:04:05.006')", "TIMESTAMP '2018-01-02T03:04:05.006'"),
            ExprFunctionTestCase("date_add(month, 1, TIMESTAMP '2017-01-02T03:04:05.006')", "TIMESTAMP '2017-02-02T03:04:05.006'"),
            ExprFunctionTestCase("date_add(day, 1, TIMESTAMP '2017-01-02T03:04:05.006')", "TIMESTAMP '2017-01-03T03:04:05.006'"),
            ExprFunctionTestCase("date_add(hour, 1, TIMESTAMP '2017-01-02T03:04:05.006')", "TIMESTAMP '2017-01-02T04:04:05.006'"),
            ExprFunctionTestCase("date_add(minute, 1, TIMESTAMP '2017-01-02T03:04:05.006')", "TIMESTAMP '2017-01-02T03:05:05.006'"),
            ExprFunctionTestCase("date_add(second, 1, TIMESTAMP '2017-01-02T03:04:05.006')", "TIMESTAMP '2017-01-02T03:04:06.006'"),

            // add enough to flip a year. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(month, 12, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2018-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(day, 365, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2018-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, ${365 * 24}, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2018-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60}, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2018-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(minute, ${365 * 24 * 60 * 60}, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2076-12-17T00:00:00'"),

            // add enough to flip a month. Skipping milliseconds as it overflows Long
            ExprFunctionTestCase("date_add(day, 31, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-02-01T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, ${31 * 24}, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-02-01T00:00:00'"),
            ExprFunctionTestCase("date_add(minute, ${31 * 24 * 60}, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-02-01T00:00:00'"),
            ExprFunctionTestCase("date_add(second, ${31 * 24 * 60 * 60}, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2017-02-01T00:00:00'"),

            // add enough to flip a day
            ExprFunctionTestCase("date_add(hour, 24, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-02-04T00:00:00'"),
            ExprFunctionTestCase("date_add(minute, ${24 * 60}, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-02-04T00:00:00'"),
            ExprFunctionTestCase("date_add(second, ${24 * 60 * 60}, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-02-04T00:00:00'"),

            // add enough to flip the hour
            ExprFunctionTestCase("date_add(minute, 60, TIMESTAMP '2017-02-04T05:06:00')", "TIMESTAMP '2017-02-04T06:06:00'"),
            ExprFunctionTestCase("date_add(second, ${60 * 60}, TIMESTAMP '2017-02-04T05:06:00')", "TIMESTAMP '2017-02-04T06:06:00'"),

            // add enough to flip the minute
            ExprFunctionTestCase("date_add(second, 60, TIMESTAMP '2017-02-04T05:06:00')", "TIMESTAMP '2017-02-04T05:07:00'"),

            // subtract 1 at different precision levels
            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2016-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2016-12-01T00:00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2016-12-31T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2016-12-31T23:00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2016-12-31T23:59:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-01-01T00:00:00')", "TIMESTAMP '2016-12-31T23:59:59'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-01T00:00:00')", "TIMESTAMP '2016-02-01T00:00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-01T00:00:00')", "TIMESTAMP '2017-01-01T00:00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-01T00:00:00')", "TIMESTAMP '2017-01-31T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-01T00:00:00')", "TIMESTAMP '2017-01-31T23:00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-01T00:00:00')", "TIMESTAMP '2017-01-31T23:59:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-01T00:00:00')", "TIMESTAMP '2017-01-31T23:59:59'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2016-02-03T00:00:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-01-03T00:00:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-02-02T00:00:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-02-02T23:00:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-02-02T23:59:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T00:00:00')", "TIMESTAMP '2017-02-02T23:59:59'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T04:05:00')", "TIMESTAMP '2016-02-03T04:05:00'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T04:05:00')", "TIMESTAMP '2017-01-03T04:05:00'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T04:05:00')", "TIMESTAMP '2017-02-02T04:05:00'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T04:05:00')", "TIMESTAMP '2017-02-03T03:05:00'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T04:05:00')", "TIMESTAMP '2017-02-03T04:04:00'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T04:05:00')", "TIMESTAMP '2017-02-03T04:04:59'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T04:05:06')", "TIMESTAMP '2016-02-03T04:05:06'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T04:05:06')", "TIMESTAMP '2017-01-03T04:05:06'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T04:05:06')", "TIMESTAMP '2017-02-02T04:05:06'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T04:05:06')", "TIMESTAMP '2017-02-03T03:05:06'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T04:05:06')", "TIMESTAMP '2017-02-03T04:04:06'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T04:05:06')", "TIMESTAMP '2017-02-03T04:05:05'"),

            ExprFunctionTestCase("date_add(year, -1, TIMESTAMP '2017-02-03T04:05:06.007')", "TIMESTAMP '2016-02-03T04:05:06.007'"),
            ExprFunctionTestCase("date_add(month, -1, TIMESTAMP '2017-02-03T04:05:06.007')", "TIMESTAMP '2017-01-03T04:05:06.007'"),
            ExprFunctionTestCase("date_add(day, -1, TIMESTAMP '2017-02-03T04:05:06.007')", "TIMESTAMP '2017-02-02T04:05:06.007'"),
            ExprFunctionTestCase("date_add(hour, -1, TIMESTAMP '2017-02-03T04:05:06.007')", "TIMESTAMP '2017-02-03T03:05:06.007'"),
            ExprFunctionTestCase("date_add(minute, -1, TIMESTAMP '2017-02-03T04:05:06.007')", "TIMESTAMP '2017-02-03T04:04:06.007'"),
            ExprFunctionTestCase("date_add(second, -1, TIMESTAMP '2017-02-03T04:05:06.007')", "TIMESTAMP '2017-02-03T04:05:05.007'")
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
