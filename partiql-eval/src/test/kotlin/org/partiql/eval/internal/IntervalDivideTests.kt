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

    @ParameterizedTest
    @MethodSource("intervalDivideNullCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalDivideNull(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalDivideMissingCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalDivideMissing(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalDivideDecimalCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalDivideDecimal(tc: SuccessTestCase) = tc.run()
    @ParameterizedTest
    @MethodSource("intervalDivideWithPrecisionOverflowCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalDivideWithPrecisionOverflowCases(tc: FailureTestCase) = tc.run()

    companion object {

        private const val INTERVAL_Y = "INTERVAL '3' YEAR"
        private const val INTERVAL_M = "INTERVAL '7' MONTH"
        private const val INTERVAL_YM = "INTERVAL '1-5' YEAR TO MONTH"

        private const val INTERVAL_D = "INTERVAL '2' DAY"
        private const val INTERVAL_H = "INTERVAL '4' HOUR"
        private const val INTERVAL_MIN = "INTERVAL '5' MINUTE"
        private const val INTERVAL_S = "INTERVAL '10.5' SECOND"
        private const val INTERVAL_DTS = "INTERVAL '2 4:5:10.5' DAY TO SECOND"
        private const val NANOS_PER_SECOND = 1000000000

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
            Input(INTERVAL_S, "3", Datum.intervalDaySecond(0, 0, 0, 3, (0.5 * NANOS_PER_SECOND).toInt(), 2, 6)),
            Input(INTERVAL_DTS, "3", Datum.intervalDaySecond(0, 17, 21, 43, (0.5 * NANOS_PER_SECOND).toInt(), 2, 6))
        ).map { case ->
            SuccessTestCase("${case.arg0} / ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalDivideNegativeCases() = listOf(
            Input(INTERVAL_Y, "-3", Datum.intervalYearMonth(-1, 0, 2)),
            Input(INTERVAL_M, "-3", Datum.intervalYearMonth(0, -2, 2)),
            Input(INTERVAL_YM, "-3", Datum.intervalYearMonth(0, -5, 2)),

            Input(INTERVAL_D, "-3", Datum.intervalDaySecond(0, -16, 0, 0, 0, 2, 6)),
            Input(INTERVAL_H, "-3", Datum.intervalDaySecond(0, -1, -20, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "-3", Datum.intervalDaySecond(0, 0, -1, -40, 0, 2, 6)),
            Input(INTERVAL_S, "-3", Datum.intervalDaySecond(0, 0, 0, -3, (-0.5 * NANOS_PER_SECOND).toInt(), 2, 6)),
            Input(INTERVAL_DTS, "-3", Datum.intervalDaySecond(0, -17, -21, -43, (-0.5 * NANOS_PER_SECOND).toInt(), 2, 6))
        ).map { case ->
            SuccessTestCase("${case.arg0} / ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalDivideDecimalCases() = listOf(
            Input(INTERVAL_Y, "2.5", Datum.intervalYearMonth(1, 2, 2)),
            Input(INTERVAL_M, "2.5", Datum.intervalYearMonth(0, 2, 2)),
            Input(INTERVAL_YM, "2.5", Datum.intervalYearMonth(0, 6, 2)),

            Input(INTERVAL_D, "2.5", Datum.intervalDaySecond(0, 19, 12, 0, 0, 2, 6)),
            Input(INTERVAL_H, "2.5", Datum.intervalDaySecond(0, 1, 36, 0, 0, 2, 6)),
            Input(INTERVAL_MIN, "2.5", Datum.intervalDaySecond(0, 0, 2, 0, 0, 2, 6)),
            Input(INTERVAL_S, "2.5", Datum.intervalDaySecond(0, 0, 0, 4, 200000000, 2, 6)),
            Input(INTERVAL_DTS, "2.5", Datum.intervalDaySecond(0, 20, 50, 4, 200000000, 2, 6))
        ).map { case ->
            SuccessTestCase("${case.arg0} / ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalDivideWithPrecisionOverflowCases() = listOf(
            "$INTERVAL_Y / 0.03", // default year precision is 2. And 3 / 0.03 will exceed the year precision and fail
            "$INTERVAL_D / 0.02" // default day precision is 2. And 2 / 0.02 will exceed the year precision and fail
        ).map { exp ->
            FailureTestCase(exp)
        }

        @JvmStatic
        fun intervalDivideNullCases() = listOf(
            Input(INTERVAL_Y, "NULL", Datum.nullValue()),
            Input(INTERVAL_M, "NULL", Datum.nullValue()),
            Input(INTERVAL_YM, "NULL", Datum.nullValue()),

            Input(INTERVAL_D, "NULL", Datum.nullValue()),
            Input(INTERVAL_H, "NULL", Datum.nullValue()),
            Input(INTERVAL_MIN, "NULL", Datum.nullValue()),
            Input(INTERVAL_S, "NULL", Datum.nullValue()),
            Input(INTERVAL_DTS, "NULL", Datum.nullValue())
        ).map { case ->
            SuccessTestCase("${case.arg0} / ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalDivideMissingCases() = listOf(
            Input(INTERVAL_Y, "MISSING", Datum.missing()),
            Input(INTERVAL_M, "MISSING", Datum.missing()),
            Input(INTERVAL_YM, "MISSING", Datum.missing()),

            Input(INTERVAL_D, "MISSING", Datum.missing()),
            Input(INTERVAL_H, "MISSING", Datum.missing()),
            Input(INTERVAL_MIN, "MISSING", Datum.missing()),
            Input(INTERVAL_S, "MISSING", Datum.missing()),
            Input(INTERVAL_DTS, "MISSING", Datum.missing())
        ).map { case ->
            SuccessTestCase("${case.arg0} / ${case.arg1}", case.expected)
        }

        @JvmStatic
        fun intervalDivideZeroCases() = listOf(
            INTERVAL_Y,
            INTERVAL_M,
            INTERVAL_YM,
            INTERVAL_D,
            INTERVAL_H,
            INTERVAL_MIN,
            INTERVAL_S,
            INTERVAL_DTS
        ).map { interval ->
            FailureTestCase("$interval / 0")
        }
    }
}
