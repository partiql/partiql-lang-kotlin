package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum

class IntervalParseSignedTests {

    @ParameterizedTest
    @MethodSource("signedIntervalCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun signedInterval(tc: SuccessTestCase) = tc.run()

    companion object {
        private const val INTERVAL_POSITIVE_M = "INTERVAL '+1' MONTH"
        private const val INTERVAL_NEGATIVE_M = "INTERVAL '-1' MONTH"
        private const val INTERVAL_POSITIVE_D = "INTERVAL '+1' DAY"
        private const val INTERVAL_NEGATIVE_D = "INTERVAL '-1' DAY"
        private const val INTERVAL_POSITIVE_H = "INTERVAL '+1' HOUR"
        private const val INTERVAL_NEGATIVE_H = "INTERVAL '-1' HOUR"
        private const val INTERVAL_POSITIVE_MIN = "INTERVAL '+1' MINUTE"
        private const val INTERVAL_NEGATIVE_MIN = "INTERVAL '-1' MINUTE"
        private const val INTERVAL_POSITIVE_S = "INTERVAL '+1.1' SECOND"
        private const val INTERVAL_NEGATIVE_S = "INTERVAL '-1.1' SECOND"
        private const val INTERVAL_POSITIVE_YM = "INTERVAL '+1-1' YEAR TO MONTH"
        private const val INTERVAL_NEGATIVE_YM = "INTERVAL '-1-1' YEAR TO MONTH"
        private const val INTERVAL_POSITIVE_DTH = "INTERVAL '+1 1' DAY TO HOUR"
        private const val INTERVAL_NEGATIVE_DTH = "INTERVAL '-1 1' DAY TO HOUR"
        private const val INTERVAL_POSITIVE_DTM = "INTERVAL '+1 1:1' DAY TO MINUTE"
        private const val INTERVAL_NEGATIVE_DTM = "INTERVAL '-1 1:1' DAY TO MINUTE"
        private const val INTERVAL_POSITIVE_DTS = "INTERVAL '+1 1:1:1.1' DAY TO SECOND"
        private const val INTERVAL_NEGATIVE_DTS = "INTERVAL '-1 1:1:1.1' DAY TO SECOND"
        private const val INTERVAL_POSITIVE_HM = "INTERVAL '+1:1' HOUR TO MINUTE"
        private const val INTERVAL_NEGATIVE_HM = "INTERVAL '-1:1' HOUR TO MINUTE"
        private const val INTERVAL_POSITIVE_HTS = "INTERVAL '+1:1:1.1' HOUR TO SECOND"
        private const val INTERVAL_NEGATIVE_HTS = "INTERVAL '-1:1:1.1' HOUR TO SECOND"
        private const val INTERVAL_POSITIVE_MTS = "INTERVAL '+1:1.1' MINUTE TO SECOND"
        private const val INTERVAL_NEGATIVE_MTS = "INTERVAL '-1:1.1' MINUTE TO SECOND"

        private const val NANO = 100_000_000 // 0.1

        @JvmStatic
        fun signedIntervalCases() = listOf(
            // parse single
            SuccessTestCase(INTERVAL_POSITIVE_M, Datum.intervalMonth(1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_M, Datum.intervalMonth(-1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_D, Datum.intervalDay(1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_D, Datum.intervalDay(-1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_H, Datum.intervalHour(1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_H, Datum.intervalHour(-1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_MIN, Datum.intervalMinute(1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_MIN, Datum.intervalMinute(-1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_S, Datum.intervalSecond(1, NANO, 2, 6)),
            SuccessTestCase(INTERVAL_NEGATIVE_S, Datum.intervalSecond(-1, -NANO, 2, 6)),

            // parse range
            SuccessTestCase(INTERVAL_POSITIVE_YM, Datum.intervalYearMonth(1, 1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_YM, Datum.intervalYearMonth(-1, -1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_DTH, Datum.intervalDayHour(1, 1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_DTH, Datum.intervalDayHour(-1, -1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_DTM, Datum.intervalDayMinute(1, 1, 1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_DTM, Datum.intervalDayMinute(-1, -1, -1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_DTS, Datum.intervalDaySecond(1, 1, 1, 1, NANO, 2, 6)),
            SuccessTestCase(INTERVAL_NEGATIVE_DTS, Datum.intervalDaySecond(-1, -1, -1, -1, -NANO, 2, 6)),
            SuccessTestCase(INTERVAL_POSITIVE_HM, Datum.intervalHourMinute(1, 1, 2)),
            SuccessTestCase(INTERVAL_NEGATIVE_HM, Datum.intervalHourMinute(-1, -1, 2)),
            SuccessTestCase(INTERVAL_POSITIVE_HTS, Datum.intervalHourSecond(1, 1, 1, NANO, 2, 6)),
            SuccessTestCase(INTERVAL_NEGATIVE_HTS, Datum.intervalHourSecond(-1, -1, -1, -NANO, 2, 6)),
            SuccessTestCase(INTERVAL_POSITIVE_MTS, Datum.intervalMinuteSecond(1, 1, NANO, 2, 6)),
            SuccessTestCase(INTERVAL_NEGATIVE_MTS, Datum.intervalMinuteSecond(-1, -1, -NANO, 2, 6))
        )
    }
}
