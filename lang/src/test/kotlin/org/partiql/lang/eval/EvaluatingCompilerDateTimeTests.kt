package org.partiql.lang.eval

import com.amazon.ion.IonStruct
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.eval.time.MINUTES_PER_HOUR
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.SECONDS_PER_MINUTE
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.getOffsetHHmm
import java.math.RoundingMode
import java.time.ZoneOffset
import kotlin.math.absoluteValue

class EvaluatingCompilerDateTimeTests : EvaluatorTestBase() {

    @Test
    fun testDateLiteral() {
        runEvaluatorTestCase(
            query = "DATE '2000-01-02'",
            expectedResult = "$DATE_ANNOTATION::2000-01-02"
        )
    }

    private fun secondsWithPrecision(time: TimeForValidation) =
        ion.newDecimal(time.second.toBigDecimal() + time.nano.toBigDecimal().divide(NANOS_PER_SECOND.toBigDecimal()).setScale(time.precision, RoundingMode.HALF_UP))

    private fun assertEqualsIonTimeStruct(actual: IonStruct, expectedTime: TimeForValidation) {
        assertEquals(ion.newInt(expectedTime.hour), actual["hour"])
        assertEquals(ion.newInt(expectedTime.minute), actual["minute"])
        assertEquals(secondsWithPrecision(expectedTime), actual["second"])
        assertEquals(ion.newInt(expectedTime.tz_minutes?.div(MINUTES_PER_HOUR)), actual["timezone_hour"])
        assertEquals(ion.newInt(expectedTime.tz_minutes?.rem(MINUTES_PER_HOUR)), actual["timezone_minute"])
    }

    data class TimeTestCase(
        val query: String,
        val expected: String,
        val expectedTime: TimeForValidation? = null,
        val compileOptionsBlock: CompileOptions.Builder.() -> Unit
    )

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForTimeLiterals::class)
    fun testTime(tc: TimeTestCase) {
        runEvaluatorTestCase(
            query = tc.query,
            expectedResult = tc.expected,
            expectedResultFormat = ExpectedResultFormat.STRING,
            compileOptionsBuilderBlock = tc.compileOptionsBlock
        ) { actualExprValue ->
            assertEquals(tc.expected, actualExprValue.toString())
            if (actualExprValue.type == ExprValueType.TIME) {
                val timeIonValue = actualExprValue.ionValue
                timeIonValue as IonStruct
                assertNotNull(tc.expectedTime)
                assertEqualsIonTimeStruct(timeIonValue, tc.expectedTime!!)
            }
        }
    }

    /**
     * Tests to visualize the behavior of evaluation of TIME literals. More tests are covered by [timeLiteralsTests].
     */
    private class ArgumentsForTimeLiterals : ArgumentsProviderBase() {
        private val defaultTimezoneOffset = ZoneOffset.UTC
        private val defaultTzMinutes = defaultTimezoneOffset.totalSeconds / 60

        private fun case(query: String, expected: String, expectedTime: TimeForValidation? = null) = TimeTestCase(query, expected, expectedTime) { }

        private fun case(query: String, expected: String, expectedTime: TimeForValidation, compileOptionsBlock: CompileOptions.Builder.() -> Unit) = TimeTestCase(query, expected, expectedTime, compileOptionsBlock)

        private fun compileOptionsBlock(hours: Int = 0, minutes: Int = 0): CompileOptions.Builder.() -> Unit = {
            defaultTimezoneOffset(ZoneOffset.ofHoursMinutes(hours, minutes))
        }

        override fun getParameters() = listOf(
            case("TIME '00:00:00.000'", "TIME '00:00:00.000'", TimeForValidation(0, 0, 0, 0, 3)),
            case("TIME '23:59:59.99999999'", "TIME '23:59:59.99999999'", TimeForValidation(23, 59, 59, 999999990, 8)),
            case("TIME (2) '23:59:59.99999999'", "TIME '00:00:00.00'", TimeForValidation(0, 0, 0, 0, 2)),
            case("TIME (2) '12:24:12.123'", "TIME '12:24:12.12'", TimeForValidation(12, 24, 12, 120000000, 2)),
            case("TIME '00:45:13.840800524'", "TIME '00:45:13.840800524'", TimeForValidation(0, 45, 13, 840800524, 9)),
            case("TIME '05:20:52.015779149'", "TIME '05:20:52.015779149'", TimeForValidation(5, 20, 52, 15779149, 9)),
            case("TIME '23:59:59'", "TIME '23:59:59'", TimeForValidation(23, 59, 59, 0, 0)),
            case("TIME (9) '12:24:12.123'", "TIME '12:24:12.123000000'", TimeForValidation(12, 24, 12, 123000000, 9)),
            case("TIME '12:24:12.12300'", "TIME '12:24:12.12300'", TimeForValidation(12, 24, 12, 123000000, 5)),
            case("TIME (3) '12:24:12.12300'", "TIME '12:24:12.123'", TimeForValidation(12, 24, 12, 123000000, 3)),
            case("TIME (4) '12:24:12.12300'", "TIME '12:24:12.1230'", TimeForValidation(12, 24, 12, 123000000, 4)),
            case("TIME (4) '12:24:12.123'", "TIME '12:24:12.1230'", TimeForValidation(12, 24, 12, 123000000, 4)),
            case("TIME (0) '12:59:59.9'", "TIME '13:00:00'", TimeForValidation(13, 0, 0, 0, 0)),
            case("TIME WITH TIME ZONE '00:00:00'", "TIME WITH TIME ZONE '00:00:00${defaultTimezoneOffset.getOffsetHHmm()}'", TimeForValidation(0, 0, 0, 0, 0, defaultTzMinutes)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123'", "TIME WITH TIME ZONE '12:24:12.12${defaultTimezoneOffset.getOffsetHHmm()}'", TimeForValidation(12, 24, 12, 120000000, 2, defaultTzMinutes)),
            case("TIME WITH TIME ZONE '12:24:12.12300'", "TIME WITH TIME ZONE '12:24:12.12300${defaultTimezoneOffset.getOffsetHHmm()}'", TimeForValidation(12, 24, 12, 123000000, 5, defaultTzMinutes)),
            case("TIME (3) WITH TIME ZONE '12:24:12.12300'", "TIME WITH TIME ZONE '12:24:12.123${defaultTimezoneOffset.getOffsetHHmm()}'", TimeForValidation(12, 24, 12, 123000000, 3, defaultTzMinutes)),
            case("TIME (4) WITH TIME ZONE '12:24:12.12300'", "TIME WITH TIME ZONE '12:24:12.1230${defaultTimezoneOffset.getOffsetHHmm()}'", TimeForValidation(12, 24, 12, 123000000, 4, defaultTzMinutes)),
            case("TIME (4) WITH TIME ZONE '12:24:12.123'", "TIME WITH TIME ZONE '12:24:12.1230${defaultTimezoneOffset.getOffsetHHmm()}'", TimeForValidation(12, 24, 12, 123000000, 4, defaultTzMinutes)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123-00:00'", "TIME WITH TIME ZONE '12:24:12.12+00:00'", TimeForValidation(12, 24, 12, 120000000, 2, 0)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123+00:00'", "TIME WITH TIME ZONE '12:24:12.12+00:00'", TimeForValidation(12, 24, 12, 120000000, 2, 0)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123+05:30'", "TIME WITH TIME ZONE '12:24:12.12+05:30'", TimeForValidation(12, 24, 12, 120000000, 2, 330)),
            case("TIME (5) WITH TIME ZONE '12:24:12.123678+05:30'", "TIME WITH TIME ZONE '12:24:12.12368+05:30'", TimeForValidation(12, 24, 12, 123680000, 5, 330)),
            case("TIME (2) WITH TIME ZONE '12:59:59.135-05:30'", "TIME WITH TIME ZONE '12:59:59.14-05:30'", TimeForValidation(12, 59, 59, 140000000, 2, -330)),
            case("TIME (2) WITH TIME ZONE '12:59:59.134-05:30'", "TIME WITH TIME ZONE '12:59:59.13-05:30'", TimeForValidation(12, 59, 59, 130000000, 2, -330)),
            case("TIME '12:25:12.123456' IS TIME", "true"),
            case("TIME (2) '01:01:12' IS TIME", "true"),
            case("TIME WITH TIME ZONE '12:25:12.123456' IS TIME", "true"),
            case("TIME (2) WITH TIME ZONE '01:01:12' IS TIME", "true"),
            case("'01:01:12' IS TIME", "false"),
            case("TIME WITH TIME ZONE '00:00:00'", "TIME WITH TIME ZONE '00:00:00-01:00'", TimeForValidation(0, 0, 0, 0, 0, -60), compileOptionsBlock(-1)),
            case("TIME WITH TIME ZONE '11:23:45.678'", "TIME WITH TIME ZONE '11:23:45.678+06:00'", TimeForValidation(11, 23, 45, 678000000, 3, 360), compileOptionsBlock(6)),
            case("TIME WITH TIME ZONE '11:23:45.678-05:30'", "TIME WITH TIME ZONE '11:23:45.678-05:30'", TimeForValidation(11, 23, 45, 678000000, 3, -330), compileOptionsBlock(6)),
            case("TIME (2) WITH TIME ZONE '12:59:59.13456'", "TIME WITH TIME ZONE '12:59:59.13-05:30'", TimeForValidation(12, 59, 59, 130000000, 2, -330), compileOptionsBlock(-5, -30))
        )
    }

    data class TimeForValidation(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val nano: Int = 0,
        val precision: Int,
        val tz_minutes: Int? = null
    ) {
        fun expectedTimeString(withTimeZone: Boolean): String {
            val timezoneMinutes = when (withTimeZone) {
                true -> tz_minutes ?: ZoneOffset.UTC.totalSeconds / SECONDS_PER_MINUTE
                else -> null
            }
            return Time.of(hour, minute, second, nano, precision, timezoneMinutes).toString()
        }

        override fun toString(): String {
            val hourStr = hour.toString().padStart(2, '0')
            val minStr = minute.toString().padStart(2, '0')
            val secStr = second.toString().padStart(2, '0')
            val nanoStr = nano.toString().padStart(9, '0')
            val timezoneStr = tz_minutes?.let {
                "" +
                    (if (it >= 0) "+" else "-") +
                    (it.absoluteValue / 60).toString().padStart(2, '0') +
                    ":" +
                    (it.absoluteValue % 60).toString().padStart(2, '0')
            } ?: ""
            return "$hourStr:$minStr:$secStr.$nanoStr$timezoneStr"
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForComparison::class)
    fun testComparison(tc: ComparisonTestCase) {
        when (tc.expected) {
            null ->
                runEvaluatorErrorTestCase(
                    query = tc.query,
                    expectedErrorCode = ErrorCode.EVALUATOR_INVALID_COMPARISION,
                    expectedPermissiveModeResult = "MISSING"
                )
            else -> {
                runEvaluatorTestCase(
                    query = tc.query,
                    expectedResult = tc.expected,
                    expectedResultFormat = ExpectedResultFormat.STRING
                )
            }
        }
    }

    /**
     * [query] is the original query to be evaluated.
     * [expected] is the expected value of the query.
     * The [null] [expected] value indicates that the comparison test case throws an error.
     */
    data class ComparisonTestCase(val query: String, val expected: String?)

    private class ArgumentsForComparison : ArgumentsProviderBase() {
        private fun case(query: String, expected: String) = ComparisonTestCase(query, expected)
        private fun errorCase(query: String) = ComparisonTestCase(query, null)
        override fun getParameters() = listOf(
            case("DATE '2012-02-29' > DATE '2012-02-28'", "true"),
            case("DATE '2012-02-29' < DATE '2013-02-28'", "true"),
            case("DATE '2012-02-29' < DATE '2012-03-29'", "true"),
            case("DATE '2012-02-29' != DATE '2012-02-29'", "false"),
            case("DATE '2012-02-29' = DATE '2012-02-29'", "true"),
            case("DATE '2012-02-29' = CAST('2012-02-29' AS DATE)", "true"),
            case("TIME '12:12:12' = TIME '12:12:12'", "true"),
            case("TIME '12:12:12' != TIME '12:12:12'", "false"),
            case("TIME '12:12:12' < TIME '12:12:12.123'", "true"),
            case("TIME '12:12:13' < TIME '12:12:12.123'", "false"),
            case("TIME WITH TIME ZONE '12:12:13' < TIME WITH TIME ZONE '12:12:12.123'", "false"),
            case("TIME WITH TIME ZONE '12:12:13' > TIME WITH TIME ZONE '12:12:12.123'", "true"),
            case("TIME WITH TIME ZONE '12:12:12.123+00:00' = TIME WITH TIME ZONE '12:12:12.123+00:00'", "true"),
            case("TIME WITH TIME ZONE '12:12:12.123-08:00' > TIME WITH TIME ZONE '12:12:12.123+00:00'", "true"),
            case("TIME WITH TIME ZONE '12:12:12.123-08:00' < TIME WITH TIME ZONE '12:12:12.123+00:00'", "false"),
            case("CAST('12:12:12.123' AS TIME WITH TIME ZONE) = TIME WITH TIME ZONE '12:12:12.123'", "true"),
            case("CAST(TIME WITH TIME ZONE '12:12:12.123' AS TIME) = TIME '12:12:12.123'", "true"),
            // Following are the error cases.
            errorCase("TIME '12:12:13' < TIME WITH TIME ZONE '12:12:12.123'"),
            errorCase("TIME WITH TIME ZONE '12:12:13' < TIME '12:12:12.123'"),
            errorCase("TIME WITH TIME ZONE '12:12:13-08:00' < TIME '12:12:12.123-08:00'"),
            errorCase("TIME WITH TIME ZONE '12:12:13' > DATE '2012-02-29'")
        )
    }
}
