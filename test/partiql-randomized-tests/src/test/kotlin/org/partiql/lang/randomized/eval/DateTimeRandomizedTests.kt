/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.lang.randomized.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZoneOffset
import kotlin.math.absoluteValue
import kotlin.random.Random

internal const val HOURS_PER_DAY = 24
internal const val MINUTES_PER_HOUR = 60
internal const val SECONDS_PER_MINUTE = 60
internal const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
internal const val NANOS_PER_SECOND = 1000000000
internal const val MAX_PRECISION_FOR_TIME = 9

class DateTimeRandomizedTests {

    class RandomTestsProvider {
        private val randomTestsSize = 50000
        private val randomGenerator = generateRandomSeed()

        private fun generateRandomSeed(): Random {
            val seed = Random.nextInt()
            println("Randomly generated seed is $seed. Use this to reproduce failures in dev environment.")
            return Random(seed)
        }

        private fun Random.nextTime(withPrecision: Boolean = false, withTimezone: Boolean = false): TimeForValidation {
            val hour = nextInt(24)
            val minute = nextInt(60)
            val second = nextInt(60)
            val nano = nextInt(999999999)
            val precision = if (withPrecision) {
                nextInt(10)
            } else {
                val timeStr = TimeForValidation(hour, minute, second, nano, 9).toString()
                timeStr.split(".")[1].length
            }
            val timezoneMinutes = if (withTimezone) {
                nextInt(-1080, 1081)
            } else {
                null
            }
            return TimeForValidation(hour, minute, second, nano, precision, timezoneMinutes)
        }
        val randomTimes = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = false,
                withTimezone = false
            )
        }
        val randomTimesWithPrecision = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = true,
                withTimezone = false
            )
        }
        val randomTimesWithTimezone = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = false,
                withTimezone = true
            )
        }
        val randomTimesWithPrecisionAndTimezone = List(randomTestsSize) {
            randomGenerator.nextTime(
                withPrecision = true,
                withTimezone = true
            )
        }
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
                true -> tz_minutes ?: (ZoneOffset.UTC.totalSeconds / SECONDS_PER_MINUTE)
                else -> null
            }
            return TimeForValidation(hour, minute, second, nano, precision, timezoneMinutes).toString()
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
    @MethodSource("randomTimes")
    fun testRandomTimes(time: TimeForValidation) {
        val query = "TIME '$time'"
        val expected = "TIME '${time.expectedTimeString(withTimeZone = false)}'"
        runEvaluatorTestCaseSuccess(
            query = query,
            expectedResult = expected
        )
    }

    @ParameterizedTest
    @MethodSource("randomTimesWithPrecision")
    fun testRandomTimesWithPrecision(time: TimeForValidation) {
        val query = "TIME (${time.precision}) '$time'"
        val expected = "TIME '${time.expectedTimeString(withTimeZone = false)}'"
        runEvaluatorTestCaseSuccess(
            query = query,
            expectedResult = expected
        )
    }

    @ParameterizedTest
    @MethodSource("randomTimesWithTimezone")
    fun testRandomTimesWithTimezone(time: TimeForValidation) {
        val query = "TIME WITH TIME ZONE '$time'"
        val expected = "TIME WITH TIME ZONE '${time.expectedTimeString(withTimeZone = true)}'"
        runEvaluatorTestCaseSuccess(
            query = query,
            expectedResult = expected
        )
    }

    @ParameterizedTest
    @MethodSource("randomTimesWithPrecisionAndTimezone")
    fun testRandomTimesWithPrecisionAndTimezone(time: TimeForValidation) {
        val query = "TIME (${time.precision}) WITH TIME ZONE '$time'"
        val expected = "TIME WITH TIME ZONE '${time.expectedTimeString(withTimeZone = true)}'"
        runEvaluatorTestCaseSuccess(
            query = query,
            expectedResult = expected
        )
    }

    companion object {
        @JvmStatic
        fun randomTimes() = RandomTestsProvider().randomTimes

        @JvmStatic
        fun randomTimesWithTimezone() = RandomTestsProvider().randomTimesWithTimezone

        @JvmStatic
        fun randomTimesWithPrecision() = RandomTestsProvider().randomTimesWithPrecision

        @JvmStatic
        fun randomTimesWithPrecisionAndTimezone() =
            RandomTestsProvider().randomTimesWithPrecisionAndTimezone
    }
}
