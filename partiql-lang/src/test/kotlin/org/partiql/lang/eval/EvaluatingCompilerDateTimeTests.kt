package org.partiql.lang.eval

import com.amazon.ion.Decimal
import com.amazon.ion.IonStruct
import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionTimestamp
import com.amazon.ionelement.api.toIonValue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ION
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.eval.evaluatortestframework.strictEquals
import org.partiql.lang.eval.time.MINUTES_PER_HOUR
import org.partiql.lang.eval.time.NANOS_PER_SECOND
import org.partiql.lang.eval.time.SECONDS_PER_MINUTE
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.getOffsetHHmm
import org.partiql.lang.util.timestampValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneOffset
import kotlin.math.absoluteValue

class EvaluatingCompilerDateTimeTests : EvaluatorTestBase() {

    // ---------------
    // |    TIME     |
    // ---------------
    private fun secondsWithPrecision(time: TimeForValidation) =
        ion.newDecimal(time.second.toBigDecimal() + time.nano.toBigDecimal().divide(NANOS_PER_SECOND.toBigDecimal()).setScale(time.precision, RoundingMode.HALF_UP))

    private fun assertEqualsIonTimeStruct(actual: IonStruct, expectedTime: TimeForValidation) {
        assertEquals(ion.newInt(expectedTime.hour), actual["hour"])
        assertEquals(ion.newInt(expectedTime.minute), actual["minute"])
        assertEquals(secondsWithPrecision(expectedTime), actual["decimalSecond"])
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
            expectedResultFormat = ExpectedResultFormat.STRICT,
            compileOptionsBuilderBlock = tc.compileOptionsBlock
        ) { actualExprValue ->
            assertEquals(tc.expected, actualExprValue.toString())
            if (actualExprValue.type == ExprValueType.TIME) {
                val timeIonValue = actualExprValue.toIonValue(ION)
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

    // ---------------
    // |  TIMESTAMP  |
    // ---------------

    private fun assertEqualsIonTimestamp(actual: IonValue, expectedTimestamp: TimestampForValidation) {
        // Has time zone, directly serialized to ion

        if (expectedTimestamp.hasTimeZone) {
            if (expectedTimestamp.precision != null) {
                actual as IonTimestamp
                assertEquals(expectedTimestamp.ionValue, actual)
            }
            // if the time stamp is arbitrary precision
            // we only want to compare if the instant refers to the same point in time.
            else {
                val expectedTimestampValue = expectedTimestamp.ionValue.timestampValue()
                val actualTimestampValue = actual.timestampValue()
                // check if time zone is known
                if (expectedTimestamp.tzHour == null && actualTimestampValue.localOffset != null) {
                    fail("Timezone mismatch, expected UNKNOWN TIME ZONE")
                }
                if (expectedTimestamp.tzHour != null && actualTimestampValue.localOffset == null) {
                    fail("Timezone mismatch, expected Known TIME ZONE")
                }
                if (expectedTimestampValue.compareTo(actualTimestampValue) != 0) {
                    println("expected: $expectedTimestampValue")
                    println("actual  : $actualTimestampValue")
                    fail("ion Timestamp value refers to different instant")
                }
            }
        } else {
            actual as IonStruct
            val expectedIon = expectedTimestamp.ionValue as IonStruct
            assertEquals(expectedIon, actual)
        }
    }
    data class TimestampTestCase(
        val queryInSqlLiteral: String,
        val queryInIonLiteral: List<String>,
        val expected: String,
        val expectedTime: TimestampForValidation? = null,
        val compileOptionsBlock: CompileOptions.Builder.() -> Unit
    )

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForTimeLstampiterals::class)
    fun testTimestamp(tc: TimestampTestCase) {
        // run evaluatorTestCase for sql query
        runEvaluatorTestCase(
            query = tc.queryInSqlLiteral,
            expectedResult = tc.expected,
            expectedResultFormat = ExpectedResultFormat.STRICT,
            compileOptionsBuilderBlock = tc.compileOptionsBlock,
            // TODO : Adding support to planner Pipeline
            target = EvaluatorTestTarget.COMPILER_PIPELINE
        ) { actualExprValueFromSql ->
            // ion serialization check
            val timestampIonValue = actualExprValueFromSql.toIonValue(ION)
            println("actualExprValueFromSql $actualExprValueFromSql")
            assertEqualsIonTimestamp(timestampIonValue, tc.expectedTime!!)

            // also, for all the equivalent ion value, the result should be the same
            tc.queryInIonLiteral.forEach { queryInIon ->
                runEvaluatorTestCase(
                    query = queryInIon,
                    expectedResult = tc.expected,
                    expectedResultFormat = ExpectedResultFormat.STRICT,
                    compileOptionsBuilderBlock = tc.compileOptionsBlock,
                    target = EvaluatorTestTarget.COMPILER_PIPELINE
                ) { actualExprValueFromIon ->
                    actualExprValueFromSql.strictEquals(actualExprValueFromIon)
                }
            }
        }
    }

    private class ArgumentsForTimeLstampiterals : ArgumentsProviderBase() {
        private val defaultTimezoneOffset = ZoneOffset.UTC
        private val defaultTzMinutes = defaultTimezoneOffset.totalSeconds / 60

        private fun case(queryInSqlLiteral: String, queryInIonLiteral: List<String>, expected: String, expectedTimestamp: TimestampForValidation? = null) =
            TimestampTestCase(queryInSqlLiteral, queryInIonLiteral, expected, expectedTimestamp) { }

        private fun case(queryInSqlLiteral: List<String>, queryInIonLiteral: List<String>, expected: String, expectedTimestamp: TimestampForValidation? = null) =
            queryInSqlLiteral.map {
                TimestampTestCase(it, queryInIonLiteral, expected, expectedTimestamp) {}
            }
        private fun case(queryInSqlLiteral: String, queryInIonLiteral: List<String>, expected: String, expectedTimestamp: TimestampForValidation, compileOptionsBlock: CompileOptions.Builder.() -> Unit) =
            TimestampTestCase(queryInSqlLiteral, queryInIonLiteral, expected, expectedTimestamp, compileOptionsBlock)

        private fun compileOptionsBlock(hours: Int = 0, minutes: Int = 0): CompileOptions.Builder.() -> Unit = {
            defaultTimezoneOffset(ZoneOffset.ofHoursMinutes(hours, minutes))
        }

        override fun getParameters() = listOf(
            // TIMESTAMP WITHOUT TIME ZONE
            // Only possible to achieve using PartiQL syntax
            case(
                "TIMESTAMP '2023-01-01 00:00:00.0000'", listOf(),
                "TIMESTAMP '2023-01-01 00:00:00.0000'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.0000"), null, null, null, false)
            ),
            // TIMESTAMP(p) WITHOUT TIME ZONE
            //   WITH exact precision
            case(
                "TIMESTAMP(4) '2023-01-01 00:00:00.0000'", listOf(),
                "TIMESTAMP(4) '2023-01-01 00:00:00.0000'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.0000"), null, null, null, false)
            ),
            // case with more than sufficent precision
            case(
                "TIMESTAMP(5) '2023-01-01 00:00:00.0000'", listOf(),
                "TIMESTAMP(5) '2023-01-01 00:00:00.00000'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.00000"), null, null, null, false)
            ),
            // case with less than sufficent precision
            case(
                "TIMESTAMP(1) '2023-01-01 00:00:00.0000'", listOf(),
                "TIMESTAMP(1) '2023-01-01 00:00:00.0'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.0"), null, null, null, false)
            ),
            // case with less than sufficent precision, require rounding
            case(
                "TIMESTAMP(1) '2023-01-01 23:59:59.999'", listOf(),
                "TIMESTAMP(1) '2023-01-02 00:00:00.0'",
                TimestampForValidation(2023, 1, 2, 0, 0, BigDecimal("00.0"), null, null, null, false)
            ),
        ) +
            // -----------------------------------------------------------------------
            // TIMESTAMP WITH TIME ZONE
            // PartiQL support two ways to declare a timestamp with time zone literal
            // The SQL style "TIMESTAMP '.....{+-}HH:MM'"
            // Or the PartiQL extension "TIMESTAMP WITH TIME ZONE '.....{+-}HH:MM'
            // -----------------------------------------------------------------------

            // TIMESTAMP WITH TIME ZONE arbitrary precision
            // Unknown TIME ZONE
            case(
                listOf("TIMESTAMP '2023-01-01 00:00:00.0-00:00'", "TIMESTAMP WITH TIME ZONE '2023-01-01 00:00:00.0-00:00'"),
                listOf("`2023T`", "`2023-01T`", "`2023-01-01T`", "`2023-01-01T00:00:00.0-00:00`"),
                "TIMESTAMP WITH TIME ZONE '2023-01-01 00:00:00.0-00:00'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.0"), null, null, null, true)
            ) +
            // Any pair of arbitrary precision timestamp
            // should be considered equal if they refer to the same point in Time
            // Meaning the decimalSecond fraction precision does not matter.
            case(
                listOf("TIMESTAMP '2023-01-01 00:00:00.0000-00:00'", "TIMESTAMP WITH TIME ZONE '2023-01-01 00:00:00.0000-00:00'"),
                listOf("`2023T`", "`2023-01T`", "`2023-01-01T`", "`2023-01-01T00:00:00.00-00:00`"),
                "TIMESTAMP WITH TIME ZONE '2023-01-01 00:00:00.0-00:00'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.0"), null, null, null, true)
            ) +
            // TIMESTAMP WITH TIME ZONE arbitrary precision
            // KNOWN timezone
            case(
                listOf("TIMESTAMP '2023-01-01 00:00:00.0000+00:00'", "TIMESTAMP WITH TIME ZONE '2023-01-01 00:00:00.0000+00:00'"),
                listOf("`2023-01-01T00:00:00.00+00:00`"),
                "TIMESTAMP WITH TIME ZONE '2023-01-01 00:00:00.0+00:00'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.0"), 0, 0, null, true)
            ) +
            // TIMESTAMP WITH TIME ZONE specified precision
            // UNKNOWN TIMEZONE, no rounding needed
            case(
                listOf("TIMESTAMP(5) '2023-01-01 00:00:00.0000-00:00'", "TIMESTAMP(5) WITH TIME ZONE '2023-01-01 00:00:00.0000-00:00'"),
                listOf(),
                "TIMESTAMP(5) WITH TIME ZONE '2023-01-01 00:00:00.00000-00:00'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.00000"), null, null, 5, true)
            ) +
            // TIMESTAMP WITH TIME ZONE arbitrary precision
            // KNOWN timezone, no rounding needed
            case(
                listOf("TIMESTAMP(5) '2023-01-01 00:00:00.0000+00:00'", "TIMESTAMP(5) WITH TIME ZONE '2023-01-01 00:00:00.0000+00:00'"),
                listOf(),
                "TIMESTAMP(5) WITH TIME ZONE '2023-01-01 00:00:00.00000+00:00'",
                TimestampForValidation(2023, 1, 1, 0, 0, BigDecimal("00.00000"), 0, 0, 5, true)
            ) +
            // TIMESTAMP WITH TIME ZONE specified precision
            // UNKNOWN TIMEZONE, rounding needed
            case(
                listOf("TIMESTAMP(5) '2023-01-01 23:59:59.999999-00:00'", "TIMESTAMP(5) WITH TIME ZONE '2023-01-01 23:59:59.999999-00:00'"),
                listOf(),
                "TIMESTAMP(5) WITH TIME ZONE '2023-01-02 00:00:00.00000-00:00'",
                TimestampForValidation(2023, 1, 2, 0, 0, BigDecimal("00.00000"), null, null, 5, true)
            ) +
            // TIMESTAMP WITH TIME ZONE arbitrary precision
            // KNOWN timezone, rounding needed
            case(
                listOf("TIMESTAMP(5) '2023-01-01 23:59:59.999999+00:00'", "TIMESTAMP(5) WITH TIME ZONE '2023-01-01 23:59:59.999999+00:00'"),
                listOf(),
                "TIMESTAMP(5) WITH TIME ZONE '2023-01-02 00:00:00.00000+00:00'",
                TimestampForValidation(2023, 1, 2, 0, 0, BigDecimal("00.00000"), 0, 0, 5, true)
            )

        // TODO: Decide what to do with TIMESTAMP WITH TIME ZONE '2023-01-01 00:00:00'
    }

    // Note
    // 1) For instance that we need to represent an unknown offset, set tzHour/tzMinutes to null, and hasTimezone to true
    // 2) For instance that has no time zone, set tzHour/TzMinutes to null and hasTimezone to false
    // 3) If tzHour is null, then Tz Minute must be null.
    data class TimestampForValidation(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: BigDecimal,
        val tzHour: Int?,
        val tzMinutes: Int?,
        val precision: Int? = null,
        val hasTimeZone: Boolean
    ) {
        init {
            if (tzHour == null && tzMinutes != null) {
                error("Offset hour value is null, but offset minute value is not null, check test cases")
            }
        }
        val ionValue = when (hasTimeZone) {
            true -> {
                val sb = StringBuilder()
                sb.append("${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}")
                sb.append("T")
                sb.append("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:")
                val (secondPart, fractionPart) = second.toPlainString().split('.', limit = 2)
                sb.append("${secondPart.padStart(2, '0')}.$fractionPart")
                when {
                    tzHour == null -> sb.append("-00:00")
                    tzHour >= 0 -> sb.append("+${tzHour.toString().padStart(2, '0')}:${tzMinutes.toString().padStart(2, '0')}")
                    else -> sb.append("${tzHour.toString().padStart(2, '0')}:${tzMinutes.toString().padStart(2, '0')}")
                }
                ionTimestamp(sb.toString()).toIonValue(ION)
            }
            false -> {
                // OR should we do ionTimestamp with annotation?
                ionStructOf(
                    field("year", ionInt(year.toLong())),
                    field("month", ionInt(month.toLong())),
                    field("day", ionInt(day.toLong())),
                    field("hour", ionInt(hour.toLong())),
                    field("minute", ionInt(minute.toLong())),
                    field("decimalSecond", ionDecimal(Decimal.valueOf(second))),
                ).withAnnotations("TIMESTAMP WITHOUT TIME ZONE").toIonValue(ION)
            }
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
                    expectedPermissiveModeResult = "MISSING",
                    target = EvaluatorTestTarget.COMPILER_PIPELINE
                )
            else -> {
                runEvaluatorTestCase(
                    query = tc.query,
                    expectedResult = tc.expected,
                    expectedResultFormat = ExpectedResultFormat.STRICT,
                    target = EvaluatorTestTarget.COMPILER_PIPELINE
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
            case("TIMESTAMP '2012-02-29 12:12:12' = TIMESTAMP '2012-02-29 12:12:12'", "true"),
            case("TIMESTAMP '2012-02-29 12:12:12.000' = TIMESTAMP '2012-02-29 12:12:12'", "true"),
            case("TIMESTAMP '2012-02-29 12:12:12.000' < TIMESTAMP '2012-02-29 13:12:12'", "true"),
            case("TIMESTAMP '2012-02-29 12:12:12.000' > TIMESTAMP '2012-02-29 11:12:12'", "true"),
            case("TIMESTAMP(1) '2012-02-29 12:12:12.000' = TIMESTAMP '2012-02-29 12:12:12'", "true"),
            case("TIMESTAMP(5) '2012-02-29 12:12:12.000' = TIMESTAMP '2012-02-29 12:12:12'", "true"),
            case("TIMESTAMP(4) '2012-02-29 12:12:11.99999' = TIMESTAMP '2012-02-29 12:12:12'", "true"),
            case("TIMESTAMP '2012-02-29 12:12:12+00:00' = TIMESTAMP WITH TIME ZONE '2012-02-29 11:12:12-01:00'", "true"),
            case("TIMESTAMP '2012-02-29 12:12:12+00:00' = TIMESTAMP WITH TIME ZONE '2012-02-29 11:12:12-01:00'", "true"),
            case("TIMESTAMP '2012-02-29 12:12:12+00:00' = TIMESTAMP '2012-02-29 12:12:12-00:00'", "true"),
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
