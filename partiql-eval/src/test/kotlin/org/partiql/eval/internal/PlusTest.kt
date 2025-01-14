package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.value.Datum
import java.math.BigDecimal

class PlusTest {

    @ParameterizedTest
    @MethodSource("plusTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun plusTests(tc: SuccessTestCase) = tc.run()

    companion object {

        // Result precision: max(s1, s2) + max(p1 - s1, p2 - s2) + 1
        // Result scale: max(s1, s2)
        @JvmStatic
        fun plusTestCases() = listOf(
            SuccessTestCase(
                input = """
                    -- DEC(10, 0) + DEC(6, 5)
                    -- P = 5 + MAX(10, 1) + 1 = 16
                    -- S = MAX(0, 5) = 5
                    1 + 2.00000;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.decimal(BigDecimal.valueOf(300000, 5), 16, 5),
                jvmEquality = true
            ),
            SuccessTestCase(
                input = """
                    -- DEC(2, 1) + DEC(6, 5)
                    -- P = 5 + MAX(1, 1) + 1 = 7
                    -- S = MAX(1, 5) = 5
                    1.0 + 2.00000;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.decimal(BigDecimal.valueOf(300000, 5), 7, 5),
                jvmEquality = true
            ),
            SuccessTestCase(
                input = """
                    -- DEC(5, 4) + DEC(6, 5)
                    -- P = 5 + MAX(1, 1) + 1 = 7
                    -- S = MAX(4, 5) = 5
                    1.0000 + 2.00000;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.decimal(BigDecimal.valueOf(300000, 5), 7, 5),
                jvmEquality = true
            ),
            SuccessTestCase(
                input = """
                    -- DEC(7, 4) + DEC(13, 7)
                    -- P = 7 + MAX(3, 6) + 1 = 14
                    -- S = MAX(4, 7) = 7
                    234.0000 + 456789.0000000;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.decimal(BigDecimal.valueOf(457023), 14, 7),
                jvmEquality = true
            ),
            SuccessTestCase(
                input = """
                    -- This shows that the value, while dynamic, still produces the right precision/scale
                    -- DEC(7, 4) + DEC(13, 7)
                    -- P = 7 + MAX(3, 6) + 1 = 14
                    -- S = MAX(4, 7) = 7
                    234.0000 + dynamic_decimal;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.decimal(BigDecimal.valueOf(457023), 14, 7),
                globals = listOf(
                    Global(
                        "dynamic_decimal",
                        "456789.0000000"
                    )
                ),
                jvmEquality = true
            ),
        )
    }
}
