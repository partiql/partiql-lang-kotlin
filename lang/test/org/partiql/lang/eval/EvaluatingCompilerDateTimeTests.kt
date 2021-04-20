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

package org.partiql.lang.eval

import com.amazon.ion.IonStruct
import com.amazon.ion.IonTimestamp
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.getOffsetHHmm
import org.partiql.lang.util.times
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.pow
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

    private fun createIonTimeStruct(value: Time): IonStruct =
        ion.newEmptyStruct().apply {
            add("hour", ion.newInt(value.hour))
            add("minute", ion.newInt(value.minute))
            add("second", ion.newFloat( value.second + value.nano * 10.0.pow(-9)))
            add("timezone_hour", ion.newInt(value.tz_minutes?.div(60)))
            add("timezone_minute", ion.newInt(value.tz_minutes?.rem(60)))
            addTypeAnnotation("\$partiql_time")
        }

    data class TimeTestCase(val query: String, val expected: String, val expectedTime: Time? = null)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForTimeLiterals::class)
    fun testTime(tc: TimeTestCase)  {
        val originalExprValue = eval(tc.query)
        assertEquals(originalExprValue.toString(), tc.expected)
        if (originalExprValue.type == ExprValueType.TIME) {
            val timeIonValue = originalExprValue.ionValue
            timeIonValue as IonStruct
            assertNotNull(tc.expectedTime)
            assertEquals("Unexpected ionStruct for time", createIonTimeStruct(value = tc.expectedTime!!), timeIonValue)
        }
    }

    /**
     * Tests to visualize the behavior of evaluation of TIME literals. More tests are covered by [timeLiteralsTests].
     */
    private class ArgumentsForTimeLiterals : ArgumentsProviderBase() {
        private val LOCAL_TIMEZONE_OFFSET = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
        private val LOCAL_TZ_MINUTES = LOCAL_TIMEZONE_OFFSET.totalSeconds / 60

        private fun case(query: String, expected: String, expectedTime: Time? = null) = TimeTestCase(query, expected, expectedTime)

        override fun getParameters() = listOf(
            case("TIME '00:00:00.000'", "00:00:00.000000000", Time(0, 0, 0)),
            case("TIME '23:59:59.99999999'", "23:59:59.999999990", Time(23, 59, 59, 999999990)),
            case("TIME (2) '23:59:59.99999999'", "00:00:00.000000000", Time(0, 0, 0)),
            case("TIME '00:45:13.840800524'", "00:45:13.840800524", Time(0, 45, 13, 840800524)),
            case("TIME '05:20:52.015779149'", "05:20:52.015779149", Time(5, 20, 52, 15779149)),
            case("TIME '23:59:59'", "23:59:59.000000000", Time(23, 59, 59)),
            case("TIME (12) '12:24:12.123'", "12:24:12.123000000", Time(12, 24, 12, 123000000)),
            case("TIME (0) '12:59:59.9'", "13:00:00.000000000", Time(13, 0,0)),
            case("TIME WITH TIME ZONE '00:00:00'", "00:00:00.000000000${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", Time(0,0,0,0,null, LOCAL_TZ_MINUTES)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123'", "12:24:12.120000000${LOCAL_TIMEZONE_OFFSET.getOffsetHHmm()}", Time(12, 24, 12, 120000000, 2, LOCAL_TZ_MINUTES)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123-00:00'", "12:24:12.120000000+00:00", Time(12, 24, 12, 120000000, 2, 0)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123+00:00'", "12:24:12.120000000+00:00", Time(12, 24, 12, 120000000, 2, 0)),
            case("TIME (2) WITH TIME ZONE '12:24:12.123+05:30'", "12:24:12.120000000+05:30", Time(12, 24, 12, 120000000, 2, 330)),
            case("TIME (2) WITH TIME ZONE '12:59:59.135-05:30'", "12:59:59.140000000-05:30", Time(12, 59, 59, 140000000, 2, -330)),
            case("TIME (2) WITH TIME ZONE '12:59:59.134-05:30'", "12:59:59.130000000-05:30", Time(12, 59, 59, 130000000, 2, -330)),
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

    data class Time(val hour: Int, val minute: Int, val second: Int, val nano: Int = 0, val precision: Int? = null, val tz_minutes: Int? = null) {
        fun expectedTimeString(withTimeZone: Boolean): String {
            val nanoWithPrecision = when  {
                precision == null || precision >= 9 -> nano
                else -> (BigDecimal(nano * 10.0.pow(-9)).setScale(precision, RoundingMode.HALF_UP) * 10.0.pow(9)).toInt()
            }
            val newNano = nanoWithPrecision % 10.0.pow(9).toInt()
            val newSecond = second + (nanoWithPrecision / 10.0.pow(9).toInt())
            return when(withTimeZone) {
                true -> {
                    val timezoneOffsetMinutes = tz_minutes ?: ZoneOffset.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 60
                    OffsetTime.of(hour, minute, newSecond, newNano, ZoneOffset.ofTotalSeconds(timezoneOffsetMinutes * 60))
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss.nnnnnnnnnxxx"))
                }
                false -> LocalTime.of(hour, minute, newSecond, newNano).format(DateTimeFormatter.ofPattern("HH:mm:ss.nnnnnnnnn"))
            }
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

    private fun Random.nextTime(withPrecision: Boolean = false, withTimezone: Boolean = false) : Time {
        val hour = nextInt(24)
        val minute = nextInt(60)
        val second = nextInt(60)
        val nano = nextInt(999999999)
        val precision = if (withPrecision) {
            nextInt(20)
        } else {
            null
        }
        val timezoneMinutes = if (withTimezone) {
            nextInt(-1080, 1081)
        } else {
            null
        }
        return Time(hour, minute, second, nano, precision, timezoneMinutes)
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
            assertEquals(expected, originalExprNode.toString())
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