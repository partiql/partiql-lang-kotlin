package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.value.Datum

class LiteralIntervalTests {
    @ParameterizedTest
    @MethodSource("literalIntervalTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun literalIntervalTests(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun literalIntervalTestCases() = listOf(
            SuccessTestCase(
                input = """
                    INTERVAL '2 5:1:2.042' DAY TO SECOND (1);
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.intervalDaySecond(2, 5, 1, 2, 40000000, 2, 1)
            ),
            SuccessTestCase(
                input = """
                    INTERVAL '2 5:1:2.042' DAY TO SECOND;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.intervalDaySecond(2, 5, 1, 2, 42000000, 2, 6)
            ),
            SuccessTestCase(
                input = """
                    INTERVAL '2' MONTH;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.intervalMonth(2, 2)
            ),
            SuccessTestCase(
                input = """
                    INTERVAL '2' MONTH (6);
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.intervalMonth(2, 6)
            ),
        )
    }
}
