package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum

class IntervalDivideTests {

    @ParameterizedTest
    @MethodSource("intervalDivideNumberCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalDivideNumber(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalDivideNegativeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalDivideNegative(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalDivideZeroCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalDivideZero(tc: FailureTestCase) = tc.run()

    companion object {

        private const val INTERVAL_Y = "INTERVAL '3' YEAR"
        private const val INTERVAL_M = "INTERVAL '7' MONTH"
        private const val INTERVAL_YM = "INTERVAL '1-5' YEAR TO MONTH"

        private const val INTERVAL_D = "INTERVAL '2' DAY"
        private const val INTERVAL_H = "INTERVAL '4' HOUR"
        private const val INTERVAL_MIN = "INTERVAL '5' MINUTE"
        private const val INTERVAL_S = "INTERVAL '10.5' SECOND"
        private const val INTERVAL_DTS = "INTERVAL '2 4:5:10.5' DAY TO SECOND"

        private class Input(
            val arg0: String,
            val arg1: String,
            val expected: Datum
        )

        @JvmStatic
        fun intervalDivideNumberCases() = listOf(
            Input(INTERVAL_Y, "3", Datum.intervalYearMonth(1, 0, 2)),
            Input(INTERVAL_M, "3", Datum.intervalYearMonth(0, 2, 2)),
            Input(INTERVAL_YM, "3", Datum.intervalYearMonth(0, 5, 2)),

            Input(INTERVAL_D, "3", Datum.intervalDaySecond(0, 16, 0, 0, 0, 2, 6)),
            Input(INTERVAL_H, "3", Datum.intervalDaySecond(0, 1, 20, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "3", Datum.intervalDaySecond(0, 0, 1, 40, 0, 2, 6)),
            Input(INTERVAL_S, "3", Datum.intervalDaySecond(0, 0, 0, 3, 5 * 1000000000, 2, 6)),
            Input(INTERVAL_DTS, "3", Datum.intervalDaySecond(0, 17, 21, 43, 5 * 1000000000, 2, 6))
        ).map { case ->
            SuccessTestCase("${case.arg0} / ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalDivideNegativeCases() = listOf(
            Input(INTERVAL_Y, "-3", Datum.intervalYearMonth(0, 0, 2)),
            Input(INTERVAL_M, "-3", Datum.intervalYearMonth(0, 0, 2)),
            Input(INTERVAL_YM, "-3", Datum.intervalYearMonth(0, 0, 2)),

            Input(INTERVAL_D, "-3", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_H, "-3", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "-3", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_S, "-3", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)),
            Input(INTERVAL_DTS, "-3", Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6))
        ).map { case ->
            SuccessTestCase("${case.arg0} / ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalDivideZeroCases() = listOf(
            INTERVAL_Y,
            INTERVAL_M,
            INTERVAL_D,
            INTERVAL_H,
            INTERVAL_MIN,
            INTERVAL_S
        ).map { interval ->
            FailureTestCase("$interval / 0")
        }
    }
}