package org.partiql.lang.eval.time

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.EvaluationException
import java.time.LocalTime
import java.time.ZoneOffset

@RunWith(JUnitParamsRunner::class)
class TimeTest {

    data class TimeTestCase(
        val hour: Int,
        val min: Int,
        val second: Int,
        val nano: Int,
        val precision: Int,
        val tz_min: Int? = null,
        val expectedLocalTime: LocalTime? = null,
        internal val expectedZoneOffset: ZoneOffset? = null,
        val expectedErrorCode: ErrorCode? = null
    )

    fun case(hour: Int, min: Int, sec: Int, nano: Int, precision: Int, tz_min: Int?, expectedLocalTime: LocalTime, expectedZoneOffset: ZoneOffset?) =
        TimeTestCase(hour, min, sec, nano, precision, tz_min, expectedLocalTime, expectedZoneOffset)

    fun case(hour: Int, min: Int, sec: Int, nano: Int, precision: Int, tz_min: Int?, expectedErrorCode: ErrorCode) =
        TimeTestCase(hour, min, sec, nano, precision, tz_min, expectedErrorCode = expectedErrorCode)

    @Test
    @Parameters
    fun runTests(tc: TimeTestCase) {
        when (tc.expectedErrorCode) {
            null -> {
                val time = Time.of(tc.hour, tc.min, tc.second, tc.nano, tc.precision, tc.tz_min)
                assertEquals(tc.expectedLocalTime, time.localTime)
                assertEquals(tc.expectedZoneOffset, time.zoneOffset)
            }
            else -> {
                try {
                    Time.of(tc.hour, tc.min, tc.second, tc.nano, tc.precision, tc.tz_min)
                } catch (e: EvaluationException) {
                    assertEquals(tc.expectedErrorCode, e.errorCode)
                }
            }
        }
    }

    fun parametersForRunTests() = listOf(
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456500,
            precision = 6,
            tz_min = null,
            expectedLocalTime = LocalTime.of(23, 23, 12, 123456000),
            expectedZoneOffset = null
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456600,
            precision = 6,
            tz_min = 300,
            expectedLocalTime = LocalTime.of(23, 23, 12, 123457000),
            expectedZoneOffset = ZoneOffset.ofTotalSeconds(300 * SECONDS_PER_MINUTE)
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456500,
            precision = 9,
            tz_min = 300,
            expectedLocalTime = LocalTime.of(23, 23, 12, 123456500),
            expectedZoneOffset = ZoneOffset.ofTotalSeconds(300 * SECONDS_PER_MINUTE)
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456789,
            precision = 0,
            tz_min = -18 * MINUTES_PER_HOUR,
            expectedLocalTime = LocalTime.of(23, 23, 12, 0),
            expectedZoneOffset = ZoneOffset.ofTotalSeconds( -18 * SECONDS_PER_HOUR)
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456789,
            precision = 0,
            tz_min = 18 * MINUTES_PER_HOUR,
            expectedLocalTime = LocalTime.of(23, 23, 12, 0),
            expectedZoneOffset = ZoneOffset.ofTotalSeconds( 18 * SECONDS_PER_HOUR)
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456789,
            precision = 0,
            tz_min = 18 * MINUTES_PER_HOUR,
            expectedErrorCode = ErrorCode.EVALUATOR_DATETIME_EXCEPTION
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456500,
            precision = 10,
            tz_min = 300,
            expectedErrorCode = ErrorCode.EVALUATOR_INVALID_PRECISION_FOR_TIME
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456500,
            precision = -1,
            tz_min = 300,
            expectedErrorCode = ErrorCode.EVALUATOR_INVALID_PRECISION_FOR_TIME
        ),
        case(
            hour = 24,
            min = 23,
            sec = 12,
            nano = 123456500,
            precision = 0,
            tz_min = 300,
            expectedErrorCode = ErrorCode.EVALUATOR_DATETIME_EXCEPTION
        ),
        case(
            hour = 23,
            min = 60,
            sec = 12,
            nano = 123456500,
            precision = 0,
            tz_min = 300,
            expectedErrorCode = ErrorCode.EVALUATOR_DATETIME_EXCEPTION
        ),
        case(
            hour = 23,
            min = 23,
            sec = 60,
            nano = 123456500,
            precision = 0,
            tz_min = 300,
            expectedErrorCode = ErrorCode.EVALUATOR_DATETIME_EXCEPTION
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456789,
            precision = 0,
            tz_min = - 18 * MINUTES_PER_HOUR - 1,
            expectedErrorCode = ErrorCode.EVALUATOR_DATETIME_EXCEPTION
        ),
        case(
            hour = 23,
            min = 23,
            sec = 12,
            nano = 123456789,
            precision = 0,
            tz_min = 18 * MINUTES_PER_HOUR + 1,
            expectedErrorCode = ErrorCode.EVALUATOR_DATETIME_EXCEPTION
        )
    )
}