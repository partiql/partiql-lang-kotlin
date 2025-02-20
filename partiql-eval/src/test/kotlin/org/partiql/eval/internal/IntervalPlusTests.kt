package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset

class IntervalPlusTests {

    @ParameterizedTest
    @MethodSource("intervalPlusDateCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusDate(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusTimeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusTime(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("timePlusIntervalFailureCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusTimeFailure(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusTimeZCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusTimeZ(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusTimestampCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusTimestamp(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusTimestampZCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusTimestampZ(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusIntervalYMCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusIntervalYM(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusIntervalDTCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusIntervalDT(tc: SuccessTestCase) = tc.run()

    companion object {

        private const val RAW_DATE = "2025-01-01"
        private const val RAW_TIME = "01:01:01.1"
        private const val RAW_OFFSET_HOUR = "01"
        private const val RAW_OFFSET_MINUTE = "30"
        private val OFFSET: ZoneOffset = ZoneOffset.ofHoursMinutes(RAW_OFFSET_HOUR.toInt(), RAW_OFFSET_MINUTE.toInt())
        private const val RAW_TIME_Z = "$RAW_TIME+$RAW_OFFSET_HOUR:$RAW_OFFSET_MINUTE"
        private const val DATE = "DATE '$RAW_DATE'"
        private const val TIME = "TIME '$RAW_TIME'"
        private const val TIMESTAMP = "TIMESTAMP '$RAW_DATE $RAW_TIME'"
        // This currently uses the WITH TIME ZONE non-SQL standard syntax. We currently don't support the standard syntax.
        private const val TIME_Z = "TIME WITH TIME ZONE '$RAW_TIME_Z'"
        private const val TIMESTAMP_Z = "TIMESTAMP WITH TIME ZONE '$RAW_DATE $RAW_TIME_Z'"
        private const val INTERVAL_Y = "INTERVAL '1' YEAR"
        private const val INTERVAL_M = "INTERVAL '1' MONTH"
        private const val INTERVAL_D = "INTERVAL '1' DAY"
        private const val INTERVAL_H = "INTERVAL '1' HOUR"
        private const val INTERVAL_MIN = "INTERVAL '1' MINUTE"
        private const val INTERVAL_S = "INTERVAL '1.1' SECOND"
        private const val INTERVAL_YM = "INTERVAL '1-1' YEAR TO MONTH"
        private const val INTERVAL_DTH = "INTERVAL '1 1' DAY TO HOUR"
        private const val INTERVAL_DTM = "INTERVAL '1 1:1' DAY TO MINUTE"
        private const val INTERVAL_DTS = "INTERVAL '1 1:1:1.1' DAY TO SECOND"
        private const val INTERVAL_HTS = "INTERVAL '1:1:1.1' HOUR TO SECOND"
        private const val INTERVAL_HM = "INTERVAL '1:1' HOUR TO MINUTE"
        private const val INTERVAL_MTS = "INTERVAL '1:1.1' MINUTE TO SECOND"
        private const val NANO = 100_000_000 // 0.1
        private const val NANOS = 200_000_000 // 0.2

        private class Input(
            val arg0: String,
            val arg1: String,
            val expected: Datum
        ) {
            constructor(arg0: String, arg1: String, expected: LocalDate) : this(arg0, arg1, Datum.date(expected))
            constructor(arg0: String, arg1: String, expected: LocalTime, precision: Int = 2) : this(arg0, arg1, Datum.time(expected, precision))
            constructor(arg0: String, arg1: String, expected: OffsetTime, precision: Int = 2) : this(arg0, arg1, Datum.timez(expected, precision))
            constructor(arg0: String, arg1: String, expected: LocalDateTime, precision: Int = 2) : this(arg0, arg1, Datum.timestamp(expected, precision))
            constructor(arg0: String, arg1: String, expected: OffsetDateTime, precision: Int = 2) : this(arg0, arg1, Datum.timestampz(expected, precision))
        }

        @JvmStatic
        fun intervalPlusDateCases() = listOf(
            Input(INTERVAL_Y, DATE, LocalDate.of(2026, 1, 1)),
            Input(INTERVAL_M, DATE, LocalDate.of(2025, 2, 1)),
            Input(INTERVAL_YM, DATE, LocalDate.of(2026, 2, 1)),
            Input(INTERVAL_D, DATE, LocalDate.of(2025, 1, 2)),
            Input(INTERVAL_DTH, DATE, LocalDate.of(2025, 1, 2)),
            Input(INTERVAL_DTM, DATE, LocalDate.of(2025, 1, 2)),
            Input(INTERVAL_DTS, DATE, LocalDate.of(2025, 1, 2)),
            Input(INTERVAL_D, DATE, LocalDate.of(2025, 1, 2)),
            Input(INTERVAL_H, DATE, LocalDate.of(2025, 1, 1)),
            Input(INTERVAL_MIN, DATE, LocalDate.of(2025, 1, 1)),
            Input(INTERVAL_S, DATE, LocalDate.of(2025, 1, 1)),
            Input(INTERVAL_DTS, DATE, LocalDate.of(2025, 1, 2)),
        ).flatMap { case ->
            listOf(
                SuccessTestCase("${case.arg0} + ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} + ${case.arg0}", case.expected),
            )
        }

        @JvmStatic
        fun intervalPlusTimeCases() = listOf(
            Input(INTERVAL_D, TIME, LocalTime.of(1, 1, 1, NANO)),
            Input(INTERVAL_H, TIME, LocalTime.of(2, 1, 1, NANO)),
            Input(INTERVAL_MIN, TIME, LocalTime.of(1, 2, 1, NANO)),
            Input(INTERVAL_S, TIME, LocalTime.of(1, 1, 2, NANOS)),
            Input(INTERVAL_DTH, TIME, LocalTime.of(2, 1, 1, NANO)),
            Input(INTERVAL_DTM, TIME, LocalTime.of(2, 2, 1, NANO)),
            Input(INTERVAL_DTS, TIME, LocalTime.of(2, 2, 2, NANOS)),
            Input(INTERVAL_HM, TIME, LocalTime.of(2, 2, 1, NANO)),
            Input(INTERVAL_HTS, TIME, LocalTime.of(2, 2, 2, NANOS)),
            Input(INTERVAL_MTS, TIME, LocalTime.of(1, 2, 2, NANOS))
        ).flatMap { case ->
            listOf(
                SuccessTestCase("${case.arg0} + ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} + ${case.arg0}", case.expected),
            )
        }

        /**
         * These _may_ actually be okay, but for now, we will mark these as unsupported. These fail (for now)!
         * I'm not entirely sure if it makes sense to add a year/month from a time.
         */
        @JvmStatic
        fun timePlusIntervalFailureCases() = listOf(
            FailureTestCase("$TIME + $INTERVAL_Y"),
            FailureTestCase("$TIME + $INTERVAL_M"),
            FailureTestCase("$TIME + $INTERVAL_YM"),
            FailureTestCase("$TIME_Z + $INTERVAL_Y"),
            FailureTestCase("$TIME_Z + $INTERVAL_M"),
            FailureTestCase("$TIME_Z + $INTERVAL_YM"),
            FailureTestCase("$INTERVAL_Y + $TIME"),
            FailureTestCase("$INTERVAL_Y + $TIME_Z"),
            FailureTestCase("$INTERVAL_M + $TIME"),
            FailureTestCase("$INTERVAL_M + $TIME_Z"),
            FailureTestCase("$INTERVAL_YM + $TIME"),
            FailureTestCase("$INTERVAL_YM + $TIME_Z"),
        )

        @JvmStatic
        fun intervalPlusTimeZCases() = listOf(
            Input(INTERVAL_D, TIME_Z, OffsetTime.of(1, 1, 1, NANO, OFFSET)), // Should this work?
            Input(INTERVAL_H, TIME_Z, OffsetTime.of(2, 1, 1, NANO, OFFSET)),
            Input(INTERVAL_MIN, TIME_Z, OffsetTime.of(1, 2, 1, NANO, OFFSET)),
            Input(INTERVAL_S, TIME_Z, OffsetTime.of(1, 1, 2, NANOS, OFFSET)),
            Input(INTERVAL_DTH, TIME_Z, OffsetTime.of(2, 1, 1, NANO, OFFSET)),
            Input(INTERVAL_DTM, TIME_Z, OffsetTime.of(2, 2, 1, NANO, OFFSET)),
            Input(INTERVAL_DTS, TIME_Z, OffsetTime.of(2, 2, 2, NANOS, OFFSET)),
            Input(INTERVAL_HM, TIME_Z, OffsetTime.of(2, 2, 1, NANO, OFFSET)),
            Input(INTERVAL_HTS, TIME_Z, OffsetTime.of(2, 2, 2, NANOS, OFFSET)),
            Input(INTERVAL_MTS, TIME_Z, OffsetTime.of(1, 2, 2, NANOS, OFFSET)),
        ).flatMap { case ->
            listOf(
                SuccessTestCase("${case.arg0} + ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} + ${case.arg0}", case.expected),
            )
        }

        @JvmStatic
        fun intervalPlusTimestampCases() = listOf(
            Input(INTERVAL_Y, TIMESTAMP, LocalDateTime.of(2026, 1, 1, 1, 1, 1, NANO)),
            Input(INTERVAL_M, TIMESTAMP, LocalDateTime.of(2025, 2, 1, 1, 1, 1, NANO)),
            Input(INTERVAL_YM, TIMESTAMP, LocalDateTime.of(2026, 2, 1, 1, 1, 1, NANO)),
            Input(INTERVAL_D, TIMESTAMP, LocalDateTime.of(2025, 1, 2, 1, 1, 1, NANO)),
            Input(INTERVAL_H, TIMESTAMP, LocalDateTime.of(2025, 1, 1, 2, 1, 1, NANO)),
            Input(INTERVAL_MIN, TIMESTAMP, LocalDateTime.of(2025, 1, 1, 1, 2, 1, NANO)),
            Input(INTERVAL_S, TIMESTAMP, LocalDateTime.of(2025, 1, 1, 1, 1, 2, NANOS)),
            Input(INTERVAL_DTH, TIMESTAMP, LocalDateTime.of(2025, 1, 2, 2, 1, 1, NANO)),
            Input(INTERVAL_DTM, TIMESTAMP, LocalDateTime.of(2025, 1, 2, 2, 2, 1, NANO)),
            Input(INTERVAL_DTS, TIMESTAMP, LocalDateTime.of(2025, 1, 2, 2, 2, 2, NANOS)),
            Input(INTERVAL_HM, TIMESTAMP, LocalDateTime.of(2025, 1, 1, 2, 2, 1, NANO)),
            Input(INTERVAL_HTS, TIMESTAMP, LocalDateTime.of(2025, 1, 1, 2, 2, 2, NANOS)),
            Input(INTERVAL_MTS, TIMESTAMP, LocalDateTime.of(2025, 1, 1, 1, 2, 2, NANOS)),
        ).flatMap { case ->
            listOf(
                SuccessTestCase("${case.arg0} + ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} + ${case.arg0}", case.expected),
            )
        }

        @JvmStatic
        fun intervalPlusTimestampZCases() = listOf(
            Input(INTERVAL_D, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 2, 1, 1, 1, NANO, OFFSET)),
            Input(INTERVAL_H, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 1, 2, 1, 1, NANO, OFFSET)),
            Input(INTERVAL_MIN, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 1, 1, 2, 1, NANO, OFFSET)),
            Input(INTERVAL_S, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 1, 1, 1, 2, NANOS, OFFSET)),
            Input(INTERVAL_DTH, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 2, 2, 1, 1, NANO, OFFSET)),
            Input(INTERVAL_DTM, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 2, 2, 2, 1, NANO, OFFSET)),
            Input(INTERVAL_DTS, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 2, 2, 2, 2, NANOS, OFFSET)),
            Input(INTERVAL_HM, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 1, 2, 2, 1, NANO, OFFSET)),
            Input(INTERVAL_HTS, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 1, 2, 2, 2, NANOS, OFFSET)),
            Input(INTERVAL_MTS, TIMESTAMP_Z, OffsetDateTime.of(2025, 1, 1, 1, 2, 2, NANOS, OFFSET)),
        ).flatMap { case ->
            listOf(
                SuccessTestCase("${case.arg0} + ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} + ${case.arg0}", case.expected),
            )
        }

        @JvmStatic
        fun intervalPlusIntervalYMCases() = listOf(
            // INTERVAL YEAR + Others
            Input(INTERVAL_Y, INTERVAL_Y, Datum.intervalYearMonth(2, 0, 2)),
            Input(INTERVAL_Y, INTERVAL_M, Datum.intervalYearMonth(1, 1, 2)),
            Input(INTERVAL_Y, INTERVAL_YM, Datum.intervalYearMonth(2, 1, 2)),

            // INTERVAL MONTH + Others
            Input(INTERVAL_M, INTERVAL_M, Datum.intervalYearMonth(0, 2, 2)),
            Input(INTERVAL_M, INTERVAL_YM, Datum.intervalYearMonth(1, 2, 2)),

            // INTERVAL YEAR TO MONTH + Others
            Input(INTERVAL_YM, INTERVAL_YM, Datum.intervalYearMonth(2, 2, 2)),
        ).flatMap { case ->
            listOf(
                SuccessTestCase("${case.arg0} + ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} + ${case.arg0}", case.expected),
            )
        }

        @JvmStatic
        fun intervalPlusIntervalDTCases() = listOf(
            // INTERVAL DAY + Others
            Input(INTERVAL_D, INTERVAL_D, Datum.intervalDaySecond(2, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_D, INTERVAL_H, Datum.intervalDaySecond(1, 1, 0, 0, 0, 2, 6)),
            Input(INTERVAL_D, INTERVAL_MIN, Datum.intervalDaySecond(1, 0, 1, 0, 0, 2, 6)),
            Input(INTERVAL_D, INTERVAL_S, Datum.intervalDaySecond(1, 0, 0, 1, NANO, 2, 6)),
            Input(INTERVAL_D, INTERVAL_DTH, Datum.intervalDaySecond(2, 1, 0, 0, 0, 2, 6)),
            Input(INTERVAL_D, INTERVAL_DTM, Datum.intervalDaySecond(2, 1, 1, 0, 0, 2, 6)),
            Input(INTERVAL_D, INTERVAL_DTS, Datum.intervalDaySecond(2, 1, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_D, INTERVAL_HM, Datum.intervalDaySecond(1, 1, 1, 0, 0, 2, 6)),
            Input(INTERVAL_D, INTERVAL_HTS, Datum.intervalDaySecond(1, 1, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_D, INTERVAL_MTS, Datum.intervalDaySecond(1, 0, 1, 1, NANO, 2, 6)),

            // INTERVAL HOUR + Others
            Input(INTERVAL_H, INTERVAL_H, Datum.intervalDaySecond(0, 2, 0, 0, 0, 2, 6)),
            Input(INTERVAL_H, INTERVAL_MIN, Datum.intervalDaySecond(0, 1, 1, 0, 0, 2, 6)),
            Input(INTERVAL_H, INTERVAL_S, Datum.intervalDaySecond(0, 1, 0, 1, NANO, 2, 6)),
            Input(INTERVAL_H, INTERVAL_HM, Datum.intervalDaySecond(0, 2, 1, 0, 0, 2, 6)),
            Input(INTERVAL_H, INTERVAL_HTS, Datum.intervalDaySecond(0, 2, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_H, INTERVAL_MTS, Datum.intervalDaySecond(0, 1, 1, 1, NANO, 2, 6)),

            // INTERVAL MINUTE + Others
            Input(INTERVAL_MIN, INTERVAL_MIN, Datum.intervalDaySecond(0, 0, 2, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, INTERVAL_S, Datum.intervalDaySecond(0, 0, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_MIN, INTERVAL_DTH, Datum.intervalDaySecond(1, 1, 1, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, INTERVAL_DTM, Datum.intervalDaySecond(1, 1, 2, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, INTERVAL_DTS, Datum.intervalDaySecond(1, 1, 2, 1, NANO, 2, 6)),
            Input(INTERVAL_MIN, INTERVAL_HM, Datum.intervalDaySecond(0, 1, 2, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, INTERVAL_HTS, Datum.intervalDaySecond(0, 1, 2, 1, NANO, 2, 6)),
            Input(INTERVAL_MIN, INTERVAL_MTS, Datum.intervalDaySecond(0, 0, 2, 1, NANO, 2, 6)),

            // INTERVAL SECOND + Others
            Input(INTERVAL_S, INTERVAL_S, Datum.intervalDaySecond(0, 0, 0, 2, NANOS, 2, 6)),
            Input(INTERVAL_S, INTERVAL_DTH, Datum.intervalDaySecond(1, 1, 0, 1, NANO, 2, 6)),
            Input(INTERVAL_S, INTERVAL_DTM, Datum.intervalDaySecond(1, 1, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_S, INTERVAL_DTS, Datum.intervalDaySecond(1, 1, 1, 2, NANOS, 2, 6)),
            Input(INTERVAL_S, INTERVAL_HM, Datum.intervalDaySecond(0, 1, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_S, INTERVAL_HTS, Datum.intervalDaySecond(0, 1, 1, 2, NANOS, 2, 6)),
            Input(INTERVAL_S, INTERVAL_MTS, Datum.intervalDaySecond(0, 0, 1, 2, NANOS, 2, 6)),

            // INTERVAL DAY TO HOUR + Others
            Input(INTERVAL_DTH, INTERVAL_DTH, Datum.intervalDaySecond(2, 2, 0, 0, 0, 2, 6)),
            Input(INTERVAL_DTH, INTERVAL_DTM, Datum.intervalDaySecond(2, 2, 1, 0, 0, 2, 6)),
            Input(INTERVAL_DTH, INTERVAL_DTS, Datum.intervalDaySecond(2, 2, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_DTH, INTERVAL_HM, Datum.intervalDaySecond(1, 2, 1, 0, 0, 2, 6)),
            Input(INTERVAL_DTH, INTERVAL_HTS, Datum.intervalDaySecond(1, 2, 1, 1, NANO, 2, 6)),
            Input(INTERVAL_DTH, INTERVAL_MTS, Datum.intervalDaySecond(1, 1, 1, 1, NANO, 2, 6)),

            // INTERVAL DAY TO MINUTE + Others
            Input(INTERVAL_DTM, INTERVAL_DTM, Datum.intervalDaySecond(2, 2, 2, 0, 0, 2, 6)),
            Input(INTERVAL_DTM, INTERVAL_DTS, Datum.intervalDaySecond(2, 2, 2, 1, NANO, 2, 6)),
            Input(INTERVAL_DTM, INTERVAL_HM, Datum.intervalDaySecond(1, 2, 2, 0, 0, 2, 6)),
            Input(INTERVAL_DTM, INTERVAL_HTS, Datum.intervalDaySecond(1, 2, 2, 1, NANO, 2, 6)),
            Input(INTERVAL_DTM, INTERVAL_MTS, Datum.intervalDaySecond(1, 1, 2, 1, NANO, 2, 6)),

            // INTERVAL DAY TO SECOND + Others
            Input(INTERVAL_DTS, INTERVAL_DTS, Datum.intervalDaySecond(2, 2, 2, 2, NANOS, 2, 6)),
            Input(INTERVAL_DTS, INTERVAL_HM, Datum.intervalDaySecond(1, 2, 2, 1, NANO, 2, 6)),
            Input(INTERVAL_DTS, INTERVAL_HTS, Datum.intervalDaySecond(1, 2, 2, 2, NANOS, 2, 6)),
            Input(INTERVAL_DTS, INTERVAL_MTS, Datum.intervalDaySecond(1, 1, 2, 2, NANOS, 2, 6)),

            // INTERVAL HOUR TO MINUTE + Others
            Input(INTERVAL_HM, INTERVAL_HM, Datum.intervalDaySecond(0, 2, 2, 0, 0, 2, 6)),
            Input(INTERVAL_HM, INTERVAL_HTS, Datum.intervalDaySecond(0, 2, 2, 1, NANO, 2, 6)),
            Input(INTERVAL_HM, INTERVAL_MTS, Datum.intervalDaySecond(0, 1, 2, 1, NANO, 2, 6)),

            // INTERVAL HOUR TO SECOND + Others
            Input(INTERVAL_HTS, INTERVAL_HTS, Datum.intervalDaySecond(0, 2, 2, 2, NANOS, 2, 6)),
            Input(INTERVAL_HTS, INTERVAL_MTS, Datum.intervalDaySecond(0, 1, 2, 2, NANOS, 2, 6)),

            // INTERVAL MINUTE TO SECOND + Others
            Input(INTERVAL_MTS, INTERVAL_MTS, Datum.intervalDaySecond(0, 0, 2, 2, NANOS, 2, 6))
        ).flatMap { case ->
            listOf(
                SuccessTestCase("${case.arg0} + ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} + ${case.arg0}", case.expected),
            )
        }
    }
}
