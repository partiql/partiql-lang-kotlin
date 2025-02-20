package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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

class IntervalMinusTests {

    @ParameterizedTest
    @MethodSource("dateMinusIntervalCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun dateMinusInterval(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("timeMinusIntervalCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun timeMinusInterval(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("timeMinusIntervalFailureCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun timeMinusIntervalFailure(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalMinusTimeZCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun timeZMinusInterval(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalMinusTimestampCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun timestampMinusInterval(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalMinusTimestampZCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun timestampZMinusInterval(tc: SuccessTestCase) = tc.run()

    @Disabled("We haven't yet implemented INTERVAL - INTERVAL yet.")
    @ParameterizedTest
    @MethodSource("intervalMinusIntervalYMCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalMinusIntervalYM(tc: SuccessTestCase) = tc.run()

    @Disabled("We haven't yet implemented INTERVAL - INTERVAL yet.")
    @ParameterizedTest
    @MethodSource("intervalMinusIntervalDTCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalMinusIntervalDT(tc: SuccessTestCase) = tc.run()

    @Test
    @Disabled("This is just a test during development. Its contents might be wrong. Uncomment to use.")
    fun developmentTest() {
        val input = """
            DATE '2025-01-01' - INTERVAL '1-1' YEAR TO MONTH
        """.trimIndent()
        val expected = Datum.date(LocalDate.of(2024, 11, 1))
        val tc = SuccessTestCase(input, expected)
        tc.run()
    }

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
        fun dateMinusIntervalCases() = listOf(
            Input(DATE, INTERVAL_Y, LocalDate.of(2024, 1, 1)),
            Input(DATE, INTERVAL_M, LocalDate.of(2024, 12, 1)),
            Input(DATE, INTERVAL_YM, LocalDate.of(2023, 12, 1)),
            Input(DATE, INTERVAL_D, LocalDate.of(2024, 12, 31)),
            Input(DATE, INTERVAL_DTH, LocalDate.of(2024, 12, 31)),
            Input(DATE, INTERVAL_DTM, LocalDate.of(2024, 12, 31)),
            Input(DATE, INTERVAL_DTS, LocalDate.of(2024, 12, 31)),
            Input(DATE, INTERVAL_D, LocalDate.of(2024, 12, 31)),
            Input(DATE, INTERVAL_H, LocalDate.of(2025, 1, 1)),
            Input(DATE, INTERVAL_MIN, LocalDate.of(2025, 1, 1)),
            Input(DATE, INTERVAL_S, LocalDate.of(2025, 1, 1)),
            Input(DATE, INTERVAL_DTS, LocalDate.of(2024, 12, 31)),
        ).map { case ->
            SuccessTestCase("${case.arg0} - ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun timeMinusIntervalCases() = listOf(
            Input(TIME, INTERVAL_D, LocalTime.of(1, 1, 1, NANO)),
            Input(TIME, INTERVAL_H, LocalTime.of(0, 1, 1, NANO)),
            Input(TIME, INTERVAL_MIN, LocalTime.of(1, 0, 1, NANO)),
            Input(TIME, INTERVAL_S, LocalTime.of(1, 1, 0, 0)),
            Input(TIME, INTERVAL_DTH, LocalTime.of(0, 1, 1, NANO)),
            Input(TIME, INTERVAL_DTM, LocalTime.of(0, 0, 1, NANO)),
            Input(TIME, INTERVAL_DTS, LocalTime.of(0, 0, 0, 0)),
            Input(TIME, INTERVAL_HM, LocalTime.of(0, 0, 1, NANO)),
            Input(TIME, INTERVAL_HTS, LocalTime.of(0, 0, 0, 0)),
            Input(TIME, INTERVAL_MTS, LocalTime.of(1, 0, 0, 0)),
            // And a complex one, for good measure
            Input(TIME, "INTERVAL '25:25:14.2' HOUR TO SECOND", LocalTime.of(23, 35, 46, 900_000_000)),
        ).map { case ->
            SuccessTestCase("${case.arg0} - ${case.arg1}", case.expected)
        }

        /**
         * These _may_ actually be okay, but for now, we will mark these as unsupported. These fail (for now)!
         * I'm not entirely sure if it makes sense to subtract a year/month from a time.
         */
        @JvmStatic
        fun timeMinusIntervalFailureCases() = listOf(
            FailureTestCase("$TIME - $INTERVAL_Y"),
            FailureTestCase("$TIME - $INTERVAL_M"),
            FailureTestCase("$TIME - $INTERVAL_YM"),
            FailureTestCase("$TIME_Z - $INTERVAL_Y"),
            FailureTestCase("$TIME_Z - $INTERVAL_M"),
            FailureTestCase("$TIME_Z - $INTERVAL_YM"),
            FailureTestCase("$INTERVAL_Y - $TIME"),
            FailureTestCase("$INTERVAL_Y - $TIME_Z"),
            FailureTestCase("$INTERVAL_M - $TIME"),
            FailureTestCase("$INTERVAL_M - $TIME_Z"),
            FailureTestCase("$INTERVAL_YM - $TIME"),
            FailureTestCase("$INTERVAL_YM - $TIME_Z"),
        )

        @JvmStatic
        fun intervalMinusTimeZCases() = listOf(
            Input(TIME_Z, INTERVAL_D, OffsetTime.of(1, 1, 1, NANO, OFFSET)), // Should this work?
            Input(TIME_Z, INTERVAL_H, OffsetTime.of(0, 1, 1, NANO, OFFSET)),
            Input(TIME_Z, INTERVAL_MIN, OffsetTime.of(1, 0, 1, NANO, OFFSET)),
            Input(TIME_Z, INTERVAL_S, OffsetTime.of(1, 1, 0, 0, OFFSET)),
            Input(TIME_Z, INTERVAL_DTH, OffsetTime.of(0, 1, 1, NANO, OFFSET)),
            Input(TIME_Z, INTERVAL_DTM, OffsetTime.of(0, 0, 1, NANO, OFFSET)),
            Input(TIME_Z, INTERVAL_DTS, OffsetTime.of(0, 0, 0, 0, OFFSET)),
            Input(TIME_Z, INTERVAL_HM, OffsetTime.of(0, 0, 1, NANO, OFFSET)),
            Input(TIME_Z, INTERVAL_HTS, OffsetTime.of(0, 0, 0, 0, OFFSET)),
            Input(TIME_Z, INTERVAL_MTS, OffsetTime.of(1, 0, 0, 0, OFFSET)),
        ).map { case ->
            SuccessTestCase("${case.arg0} - ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalMinusTimestampCases() = listOf(
            Input(TIMESTAMP, INTERVAL_Y, LocalDateTime.of(2024, 1, 1, 1, 1, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_M, LocalDateTime.of(2024, 12, 1, 1, 1, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_YM, LocalDateTime.of(2023, 12, 1, 1, 1, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_D, LocalDateTime.of(2024, 12, 31, 1, 1, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_H, LocalDateTime.of(2025, 1, 1, 0, 1, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_MIN, LocalDateTime.of(2025, 1, 1, 1, 0, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_S, LocalDateTime.of(2025, 1, 1, 1, 1, 0, 0)),
            Input(TIMESTAMP, INTERVAL_DTH, LocalDateTime.of(2024, 12, 31, 0, 1, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_DTM, LocalDateTime.of(2024, 12, 31, 0, 0, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_DTS, LocalDateTime.of(2024, 12, 31, 0, 0, 0, 0)),
            Input(TIMESTAMP, INTERVAL_HM, LocalDateTime.of(2025, 1, 1, 0, 0, 1, NANO)),
            Input(TIMESTAMP, INTERVAL_HTS, LocalDateTime.of(2025, 1, 1, 0, 0, 0, 0)),
            Input(TIMESTAMP, INTERVAL_MTS, LocalDateTime.of(2025, 1, 1, 1, 0, 0, 0)),
            // And a complex one, for good measure
            Input(TIMESTAMP, "INTERVAL '25:25:14.2' HOUR TO SECOND", LocalDateTime.of(2024, 12, 30, 23, 35, 46, 900_000_000)),
        ).map { case ->
            SuccessTestCase("${case.arg0} - ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalMinusTimestampZCases() = listOf(
            Input(TIMESTAMP_Z, INTERVAL_D, OffsetDateTime.of(2024, 12, 31, 1, 1, 1, NANO, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_H, OffsetDateTime.of(2025, 1, 1, 0, 1, 1, NANO, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_MIN, OffsetDateTime.of(2025, 1, 1, 1, 0, 1, NANO, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_S, OffsetDateTime.of(2025, 1, 1, 1, 1, 0, 0, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_DTH, OffsetDateTime.of(2024, 12, 31, 0, 1, 1, NANO, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_DTM, OffsetDateTime.of(2024, 12, 31, 0, 0, 1, NANO, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_DTS, OffsetDateTime.of(2024, 12, 31, 0, 0, 0, 0, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_HM, OffsetDateTime.of(2025, 1, 1, 0, 0, 1, NANO, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_HTS, OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, OFFSET)),
            Input(TIMESTAMP_Z, INTERVAL_MTS, OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, OFFSET)),
        ).map { case ->
            SuccessTestCase("${case.arg0} - ${case.arg1}", case.expected)
        }

        // These tests aren't correct. They're disabled anyway and are just a placeholder.
        @JvmStatic
        fun intervalMinusIntervalYMCases() = listOf(
            // INTERVAL YEAR + Others
            Input(INTERVAL_Y, INTERVAL_Y, Datum.intervalYearMonth(2, 0, 2)),
            Input(INTERVAL_Y, INTERVAL_M, Datum.intervalYearMonth(1, 1, 2)),
            Input(INTERVAL_Y, INTERVAL_YM, Datum.intervalYearMonth(2, 1, 2)),

            // INTERVAL MONTH + Others
            Input(INTERVAL_M, INTERVAL_M, Datum.intervalYearMonth(0, 2, 2)),
            Input(INTERVAL_M, INTERVAL_YM, Datum.intervalYearMonth(1, 2, 2)),

            // INTERVAL YEAR TO MONTH + Others
            Input(INTERVAL_YM, INTERVAL_YM, Datum.intervalYearMonth(2, 2, 2)),
        ).map { case ->
            SuccessTestCase("${case.arg0} - ${case.arg1}", case.expected)
        }

        // These tests aren't correct. They're disabled anyway and are just a placeholder.
        @JvmStatic
        fun intervalMinusIntervalDTCases() = listOf(
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
                SuccessTestCase("${case.arg0} - ${case.arg1}", case.expected),
                SuccessTestCase("${case.arg1} - ${case.arg0}", case.expected),
            )
        }
    }
}
