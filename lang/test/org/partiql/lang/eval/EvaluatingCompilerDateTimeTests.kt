package org.partiql.lang.eval

import com.amazon.ion.IonStruct
import com.amazon.ion.IonTimestamp
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.time.*
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.getOffsetHHmm
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue
import kotlin.random.Random

class EvaluatingCompilerDateTimeTests : EvaluatorTestBase() {

    private val RANDOM_TESTS_SIZE = 200

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForDateLiterals::class)
    fun testDate(tc: EvaluatorTestCase)  {
        val originalExprValue = eval(tc.sqlUnderTest)
        assertEquals(originalExprValue.toString(), tc.expectedSql)
        if (originalExprValue.type == ExprValueType.DATE) {
            val (year, month, day) = tc.expectedSql.split("-")
            val dateIonValue = originalExprValue.ionValue
            dateIonValue as IonTimestamp
            val timestamp = dateIonValue.timestampValue()
            assertEquals("Expected year to be $year", year.toInt(), timestamp.year)
            assertEquals("Expected month to be $month", month.toInt(), timestamp.month)
            assertEquals("Expected day to be $day", day.toInt(), timestamp.day)
        }
    }

    private class ArgumentsForDateLiterals : ArgumentsProviderBase() {
        private fun case(query: String, expected: String) = EvaluatorTestCase(query, expected)

        override fun getParameters() = listOf(
            case("DATE '2012-02-29'", "2012-02-29"),
            case("DATE '2021-02-28'", "2021-02-28"),
            case("DATE '2021-03-17' IS DATE", "true"),
            case("'2021-03-17' IS DATE", "false")
        )
    }

    private fun secondsWithPrecision(time: TimeForTest) =
        ion.newDecimal(BigDecimal(time.second + time.nano / NANOS_PER_SECOND).setScale(time.precision, RoundingMode.HALF_UP))

    private fun assertEqualsIonTimeStruct(actual: IonStruct, expectedTime: TimeForTest) {
        assertEquals(ion.newInt(expectedTime.hour), actual["hour"])
        assertEquals(ion.newInt(expectedTime.minute), actual["minute"])
        assertEquals(secondsWithPrecision(expectedTime), actual["second"])
        assertEquals(ion.newInt(expectedTime.tz_minutes?.div(MINUTES_PER_HOUR)), actual["timezone_hour"])
        assertEquals(ion.newInt(expectedTime.tz_minutes?.rem(MINUTES_PER_HOUR)), actual["timezone_minute"])
    }

    data class TimeTestCase(val query: String, val expected: String, val expectedTime: TimeForTest? = null)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForTimeLiterals::class)
    fun testTime(tc: TimeTestCase)  {
        val originalExprValue = eval(tc.query)
        assertEquals(tc.expected, originalExprValue.toString())
        if (originalExprValue.type == ExprValueType.TIME) {
            val timeIonValue = originalExprValue.ionValue
            timeIonValue as IonStruct
            assertNotNull(tc.expectedTime)
            assertEqualsIonTimeStruct(timeIonValue, tc.expectedTime!!)
        }
    }

    /**
     * Tests to visualize the behavior of evaluation of TIME literals. More tests are covered by [timeLiteralsTests].
     */
    private class ArgumentsForTimeLiterals : ArgumentsProviderBase() {
        private val LOCAL_TIMEZONE_OFFSET = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
        private val LOCAL_TZ_MINUTES = LOCAL_TIMEZONE_OFFSET.totalSeconds / 60

        private fun case(query: String, expected: String, expectedTime: TimeForTest? = null) = TimeTestCase(query, expected, expectedTime)

        override fun getParameters() = listOf(
            case("TIME '00:00:00.000'", "00:00:00.000", TimeForTest(0, 0, 0, 0, 3)),
            case("TIME '23:59:59.99999999'", "23:59:59.99999999", TimeForTest(23, 59, 59, 999999990, 8)),
            case("TIME (2) '23:59:59.99999999'", "00:00:00.00", TimeForTest(0, 0, 0, 0, 2)),
            case("TIME (2) '12:24:12.123'", "12:24:12.12", TimeForTest(12, 24, 12, 120000000, 2)),
            case("TIME '00:45:13.840800524'", "00:45:13.840800524", TimeForTest(0, 45, 13, 840800524, 9)),
            case("TIME '05:20:52.015779149'", "05:20:52.015779149", TimeForTest(5, 20, 52, 15779149, 9)),
            case("TIME '23:59:59'", "23:59:59", TimeForTest(23, 59, 59, 0, 0)),
            case("TIME (9) '12:24:12.123'", "12:24:12.123000000", TimeForTest(12, 24, 12, 123000000, 9)),
            case("TIME '12:24:12.12300'", "12:24:12.12300", TimeForTest(12, 24, 12, 123000000, 5)),
            case("TIME (3) '12:24:12.12300'", "12:24:12.123", TimeForTest(12, 24, 12, 123000000, 3)),
            case("TIME (4) '12:24:12.12300'", "12:24:12.1230", TimeForTest(12, 24, 12, 123000000, 4)),
            case("TIME (4) '12:24:12.123'", "12:24:12.1230", TimeForTest(12, 24, 12, 123000000, 4)),
            case("TIME (0) '12:59:59.9'", "13:00:00", TimeForTest(13, 0,0, 0, 0)),
            case("TIME WITH TIME ZONE '00:00:00'", "00:00:00${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", TimeForTest(0,0,0,0,0, LOCAL_TZ_MINUTES)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123'", "12:24:12.12${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", TimeForTest(12, 24, 12, 120000000, 2, LOCAL_TZ_MINUTES)),
            case("TIME WITH TIME ZONE '12:24:12.12300'", "12:24:12.12300${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", TimeForTest(12, 24, 12, 123000000, 5, LOCAL_TZ_MINUTES)),
            case("TIME (3) WITH TIME ZONE '12:24:12.12300'", "12:24:12.123${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", TimeForTest(12, 24, 12, 123000000, 3, LOCAL_TZ_MINUTES)),
            case("TIME (4) WITH TIME ZONE '12:24:12.12300'", "12:24:12.1230${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", TimeForTest(12, 24, 12, 123000000, 4, LOCAL_TZ_MINUTES)),
            case("TIME (4) WITH TIME ZONE '12:24:12.123'", "12:24:12.1230${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", TimeForTest(12, 24, 12, 123000000, 4, LOCAL_TZ_MINUTES)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123-00:00'", "12:24:12.12+00:00", TimeForTest(12, 24, 12, 120000000, 2, 0)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123+00:00'", "12:24:12.12+00:00", TimeForTest(12, 24, 12, 120000000, 2, 0)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123+05:30'", "12:24:12.12+05:30", TimeForTest(12, 24, 12, 120000000, 2, 330)),
            case("TIME (5) WITH TIME ZONE '12:24:12.123678+05:30'", "12:24:12.12368+05:30", TimeForTest(12, 24, 12, 123680000, 5, 330)),
            case("TIME (2) WITH TIME ZONE '12:59:59.135-05:30'", "12:59:59.14-05:30", TimeForTest(12, 59, 59, 140000000, 2, -330)),
            case("TIME (2) WITH TIME ZONE '12:59:59.134-05:30'", "12:59:59.13-05:30", TimeForTest(12, 59, 59, 130000000, 2, -330)),
            case("TIME '12:25:12.123456' IS TIME", "true"),
            case("TIME (2) '01:01:12' IS TIME", "true"),
            case("TIME WITH TIME ZONE '12:25:12.123456' IS TIME", "true"),
            case("TIME (2) WITH TIME ZONE '01:01:12' IS TIME", "true"),
            case("'01:01:12' IS TIME", "false")
        )
    }

    private val RANDOM_GENERATOR = generateRandomSeed()

    private fun generateRandomSeed() : Random {
        val seed = Random(1).nextInt()
        println("Randomly generated seed is ${seed}. Use this to reproduce failures in dev environment.")
        return Random(seed)
    }

    data class TimeForTest(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val nano: Int = 0,
        val precision: Int,
        val tz_minutes: Int? = null
    ) {
        fun expectedTimeString(withTimeZone: Boolean): String {
            val timezoneMinutes = when(withTimeZone) {
                true -> tz_minutes ?: ZoneOffset.systemDefault().rules.getOffset(Instant.now()).totalSeconds / SECONDS_PER_MINUTE
                else -> null
            }
            return Time.of(hour, minute, second, nano, precision, timezoneMinutes).toString()
        }

        override fun toString(): String {
            val hourStr = hour.toString().padStart(2, '0')
            val minStr = minute.toString().padStart(2,'0')
            val secStr = second.toString().padStart(2, '0')
            val nanoStr = nano.toString().padStart(9, '0')
            val timezoneStr = tz_minutes?.let { "" +
                (if (it >= 0) "+" else "-") +
                (it.absoluteValue / 60).toString().padStart(2, '0') +
                ":" +
                (it.absoluteValue % 60).toString().padStart(2, '0')
            } ?: ""
            return "$hourStr:$minStr:$secStr.$nanoStr$timezoneStr"
        }
    }

    private fun Random.nextTime(withPrecision: Boolean = false, withTimezone: Boolean = false) : TimeForTest {
        val hour = nextInt(24)
        val minute = nextInt(60)
        val second = nextInt(60)
        val nano = nextInt(999999999)
        val precision = if (withPrecision) {
            nextInt(10)
        } else {
            val timeStr = LocalTime.of(hour, minute, second, nano).toString()
            timeStr.split(".")[1].length
        }
        val timezoneMinutes = if (withTimezone) {
            nextInt(-1080, 1081)
        } else {
            null
        }
        return TimeForTest(hour, minute, second, nano, precision, timezoneMinutes)
    }

    private val RANDOM_TIMES = List(RANDOM_TESTS_SIZE) {
        RANDOM_GENERATOR.nextTime(
            withPrecision = false,
            withTimezone = false
        )
    }
    private val RANDOM_TIMES_WITH_PRECISION = List(RANDOM_TESTS_SIZE) {
        RANDOM_GENERATOR.nextTime(
            withPrecision = true,
            withTimezone = false
        )
    }
    private val RANDOM_TIMES_WITH_TIMEZONE = List(RANDOM_TESTS_SIZE) {
        RANDOM_GENERATOR.nextTime(
            withPrecision = false,
            withTimezone = true
        )
    }
    private val RANDOM_TIMES_WITH_PRECISION_AND_TIMEZONE = List(RANDOM_TESTS_SIZE) {
        RANDOM_GENERATOR.nextTime(
            withPrecision = true,
            withTimezone = true
        )
    }

    @Test
    fun testRandomTimes() {
        (RANDOM_TIMES + RANDOM_TIMES_WITH_TIMEZONE).map {
            val query = "TIME '$it'"
            val expected = it.expectedTimeString(withTimeZone = false)
            val originalExprNode = eval(query)
            assertEquals("Query $query failed.", expected, originalExprNode.toString())
        }
    }

    @Test
    fun testRandomTimesWithPrecision() {
         (RANDOM_TIMES_WITH_PRECISION + RANDOM_TIMES_WITH_PRECISION_AND_TIMEZONE).map {
            val query = "TIME (${it.precision}) '$it'"
            val expected = it.expectedTimeString(withTimeZone = false)
            val originalExprNode = eval(query)
            assertEquals(expected, originalExprNode.toString())
        }
    }

    @Test
    fun testRandomTimesWithTimezone() {
         (RANDOM_TIMES + RANDOM_TIMES_WITH_TIMEZONE).map {
            val query = "TIME WITH TIME ZONE '$it'"
            val expected = it.expectedTimeString(withTimeZone = true)
            val originalExprNode = eval(query)
            assertEquals(expected, originalExprNode.toString())
        }
    }

    @Test
    fun testRandomTimesWithPrecisionAndTimezone() {
        (RANDOM_TIMES_WITH_PRECISION + RANDOM_TIMES_WITH_PRECISION_AND_TIMEZONE).map {
            val query = "TIME (${it.precision}) WITH TIME ZONE '$it'"
            val expected = it.expectedTimeString(withTimeZone = true)
            val originalExprNode = eval(query)
            assertEquals(expected, originalExprNode.toString())
        }
    }

}