package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset

class IntervalOperationTests {

    @ParameterizedTest
    @MethodSource("intervalPlusDateCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusDate(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusTimeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusTime(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalPlusTimeZCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalPlusTimeZ(tc: SuccessTestCase) = tc.run()

    companion object {
        @JvmStatic
        fun intervalPlusDateCases() = listOf(
            SuccessTestCase(
                "INTERVAL '1' YEAR + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2026, 1, 1))
            ),
            SuccessTestCase(
                "INTERVAL '1' MONTH + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2025, 2, 1))
            ),
            SuccessTestCase(
                "INTERVAL '1-2' YEAR TO MONTH + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2026, 3, 1))
            ),
            SuccessTestCase(
                "INTERVAL '1' DAY + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2025, 1, 2))
            ),
            SuccessTestCase(
                "INTERVAL '1' HOUR + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2025, 1, 1))
            ),
            SuccessTestCase(
                "INTERVAL '1' MINUTE + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2025, 1, 1))
            ),
            SuccessTestCase(
                "INTERVAL '1' SECOND + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2025, 1, 1))
            ),
            SuccessTestCase(
                "INTERVAL '1 2:3:4.5' DAY TO SECOND + DATE '2025-01-01'",
                Datum.date(LocalDate.of(2025, 1, 2))
            ),
        )

        @JvmStatic
        fun intervalPlusTimeCases() = listOf(
            SuccessTestCase(
                "INTERVAL '1' DAY + TIME '01:01:01'", // TODO: Should this work?
                Datum.time(LocalTime.of(1, 1, 1), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1' HOUR + TIME '01:01:01'",
                Datum.time(LocalTime.of(2, 1, 1), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1' MINUTE + TIME '01:01:01'",
                Datum.time(LocalTime.of(1, 2, 1), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1' SECOND + TIME '01:01:01'",
                Datum.time(LocalTime.of(1, 1, 2), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1:2' HOUR TO MINUTE + TIME '01:01:01'",
                Datum.time(LocalTime.of(2, 3, 1), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1:2:3.4' HOUR TO SECOND + TIME '01:01:01'",
                Datum.time(LocalTime.of(2, 3, 4, 400000000), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1:2.3' MINUTE TO SECOND + TIME '01:01:01'",
                Datum.time(LocalTime.of(1, 2, 3, 300000000), 2)
            )
        )

        @JvmStatic
        private val OFFSET_08_30: ZoneOffset = ZoneOffset.ofHoursMinutes(8, 30)

        // TODO: This currently uses the WITH TIME ZONE non-SQL standard syntax. We currently don't support the standard syntax.
        @JvmStatic
        fun intervalPlusTimeZCases() = listOf(
            SuccessTestCase(
                "INTERVAL '1' DAY + TIME WITH TIME ZONE '01:01:01+08:30'", // TODO: Should this work?
                Datum.timez(OffsetTime.of(1, 1, 1, 0, OFFSET_08_30), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1' HOUR + TIME WITH TIME ZONE '01:01:01+08:30'",
                Datum.timez(OffsetTime.of(2, 1, 1, 0, OFFSET_08_30), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1' MINUTE + TIME WITH TIME ZONE '01:01:01+08:30'",
                Datum.timez(OffsetTime.of(1, 2, 1, 0, OFFSET_08_30), 2),
            ),
            SuccessTestCase(
                "INTERVAL '1' SECOND + TIME WITH TIME ZONE '01:01:01+08:30'",
                Datum.timez(OffsetTime.of(1, 1, 2, 0, OFFSET_08_30), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1:2' HOUR TO MINUTE + TIME WITH TIME ZONE '01:01:01+08:30'",
                Datum.timez(OffsetTime.of(2, 3, 1, 0, OFFSET_08_30), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1:2:3.4' HOUR TO SECOND + TIME WITH TIME ZONE '01:01:01+08:30'",
                Datum.timez(OffsetTime.of(2, 3, 4, 400000000, OFFSET_08_30), 2)
            ),
            SuccessTestCase(
                "INTERVAL '1:2.3' MINUTE TO SECOND + TIME WITH TIME ZONE '01:01:01+08:30'",
                Datum.timez(OffsetTime.of(1, 2, 3, 300000000, OFFSET_08_30), 2)
            )
        )
    }
}
