package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum

class IntervalTimesTests {

    @ParameterizedTest
    @MethodSource("intervalTimesNumberCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalTimesNumber(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("numberTimesIntervalCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun numberTimesInterval(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalTimesZeroCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalTimesZero(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalTimesNegativeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalTimesNegative(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalTimesDecimalCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalTimesDecimal(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalTimesWithOverflowCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalTimesNine(tc: SuccessTestCase) = tc.run()

    companion object {

        private const val INTERVAL_Y = "INTERVAL '3' YEAR"
        private const val INTERVAL_M = "INTERVAL '7' MONTH"
        private const val INTERVAL_YM = "INTERVAL '1-5' YEAR TO MONTH"
        private const val INTERVAL_YM_LARGE = "INTERVAL '111111-1' YEAR(6) TO MONTH"
        private const val INTERVAL_D = "INTERVAL '2' DAY"
        private const val INTERVAL_H = "INTERVAL '4' HOUR"
        private const val INTERVAL_MIN = "INTERVAL '5' MINUTE"
        private const val INTERVAL_S = "INTERVAL '10.5' SECOND"
        private const val INTERVAL_DTS = "INTERVAL '2 4:5:10.5' DAY TO SECOND"
        private const val INTERVAL_DTS_LARGE = "INTERVAL '111111 1:1:1.111111' DAY(6) TO SECOND(6)"
        private const val NANOS_PER_SECOND = 1_000_000_000

        private class Input(
            val arg0: String,
            val arg1: String,
            val expected: Datum
        )

        @JvmStatic
        fun intervalTimesNumberCases() = listOf(
            Input(INTERVAL_Y, "2", Datum.intervalYearMonth(6, 0, 2)),
            Input(INTERVAL_M, "2", Datum.intervalYearMonth(1, 2, 2)),
            Input(INTERVAL_YM, "2", Datum.intervalYearMonth(2, 10, 2)),

            Input(INTERVAL_D, "20", Datum.intervalDaySecond(40, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_H, "20", Datum.intervalDaySecond(3, 8, 0, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "20", Datum.intervalDaySecond(0, 1, 40, 0, 0, 2, 6)),
            Input(INTERVAL_S, "20", Datum.intervalDaySecond(0, 0, 3, 30, 0, 2, 6)),
            Input(INTERVAL_DTS, "20", Datum.intervalDaySecond(43, 9, 43, 30, 0, 2, 6)),
        ).map { case ->
            SuccessTestCase("${case.arg0} * ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun numberTimesIntervalCases() = listOf(
            Input("2", INTERVAL_Y, Datum.intervalYearMonth(6, 0, 2)),
            Input("2", INTERVAL_M, Datum.intervalYearMonth(1, 2, 2)),
            Input("2", INTERVAL_YM, Datum.intervalYearMonth(2, 10, 2)),

            Input("20", INTERVAL_D, Datum.intervalDaySecond(40, 0, 0, 0, 0, 2, 6)),
            Input("20", INTERVAL_H, Datum.intervalDaySecond(3, 8, 0, 0, 0, 2, 6)),
            Input("20", INTERVAL_MIN, Datum.intervalDaySecond(0, 1, 40, 0, 0, 2, 6)),
            Input("20", INTERVAL_S, Datum.intervalDaySecond(0, 0, 3, 30, 0, 2, 6)),
            Input("20", INTERVAL_DTS, Datum.intervalDaySecond(43, 9, 43, 30, 0, 2, 6)),
        ).map { case ->
            SuccessTestCase("${case.arg0} * ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalTimesZeroCases() = listOf(
            Input(INTERVAL_Y, "0", Datum.intervalYearMonth(0, 0, 2)),
            Input(INTERVAL_M, "0", Datum.intervalYearMonth(0, 0, 2)),
            Input(INTERVAL_YM, "0", Datum.intervalYearMonth(0, 0, 2)),

            Input(INTERVAL_D, "0", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_H, "0", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "0", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_S, "0", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_DTS, "0", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6))
        ).map { case ->
            SuccessTestCase("${case.arg0} * ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalTimesNegativeCases() = listOf(
            Input(INTERVAL_Y, "-2", Datum.intervalYearMonth(-6, 0, 2)),
            Input(INTERVAL_M, "-2", Datum.intervalYearMonth(-1, -2, 2)),
            Input(INTERVAL_YM, "-2", Datum.intervalYearMonth(-2, -10, 2)),

            Input(INTERVAL_D, "-20", Datum.intervalDaySecond(-40, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_H, "-20", Datum.intervalDaySecond(-3, -8, 0, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "-20", Datum.intervalDaySecond(0, -1, -40, 0, 0, 2, 6)),
            Input(INTERVAL_S, "-20", Datum.intervalDaySecond(0, 0, -3, -30, 0, 2, 6)),
            Input(INTERVAL_DTS, "-20", Datum.intervalDaySecond(-43, -9, -43, -30, 0, 2, 6)),
        ).map { case ->
            SuccessTestCase("${case.arg0} * ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalTimesDecimalCases() = listOf(
            Input(INTERVAL_Y, "2.1", Datum.intervalYearMonth(6, 3, 2)),
            Input(INTERVAL_M, "2.1", Datum.intervalYearMonth(1, 2, 2)),
            Input(INTERVAL_YM, "2.1", Datum.intervalYearMonth(2, 11, 2)),

            Input(INTERVAL_D, "2.1", Datum.intervalDaySecond(4, 4, 48, 0, 0, 2, 6)),
            Input(INTERVAL_H, "2.1", Datum.intervalDaySecond(0, 8, 24, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "2.1", Datum.intervalDaySecond(0, 0, 10, 30, 0, 2, 6)),
            Input(INTERVAL_S, "2.1", Datum.intervalDaySecond(0, 0, 0, 22, (0.05 * NANOS_PER_SECOND).toInt(), 2, 6)),
            Input(INTERVAL_DTS, "2.1", Datum.intervalDaySecond(4, 13, 22, 52, (0.05 * NANOS_PER_SECOND).toInt(), 2, 6)),
        ).map { case ->
            SuccessTestCase("${case.arg0} * ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalTimesWithOverflowCases() = listOf(
            Input(INTERVAL_YM_LARGE, "9", Datum.intervalYearMonth(999999, 9, 6)),
            Input(INTERVAL_DTS_LARGE, "9", Datum.intervalDaySecond(999999, 9, 9, 9, 999999000, 6, 6)),
        ).map { case ->
            SuccessTestCase("${case.arg0} * ${case.arg1}", case.expected)
        }
    }
}
