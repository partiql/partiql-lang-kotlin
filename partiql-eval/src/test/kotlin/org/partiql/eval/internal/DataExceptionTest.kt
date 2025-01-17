package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * E2E evaluation tests that give a data exception.
 */
class DataExceptionTest {

    @ParameterizedTest
    @MethodSource("sandboxT")
    @Execution(ExecutionMode.CONCURRENT)
    fun sandbox(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("plusOverflowTests")
    @Execution(ExecutionMode.CONCURRENT)
    fun plusOverflow(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("minusOverflowTests")
    @Execution(ExecutionMode.CONCURRENT)
    fun minusOverflow(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("timesOverflowTests")
    @Execution(ExecutionMode.CONCURRENT)
    fun timesOverflow(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("divideTests")
    @Execution(ExecutionMode.CONCURRENT)
    fun divideOverflow(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("divideByZeroTests")
    fun divideByZero(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("modByZeroTests")
    fun modByZero(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("absOverflowTests")
    fun absOverflow(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("negOverflowTests")
    fun negOverflow(tc: FailureTestCase) = tc.run()

    companion object {
        @JvmStatic
        fun sandboxT() = listOf(
            // TINYINT
            FailureTestCase(
                input = "SELECT x + 1 FROM << 1, 2e0, MISSING>> AS x;"
            )
        )

        @JvmStatic
        fun plusOverflowTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "CAST(${Byte.MAX_VALUE} AS TINYINT) + CAST(1 AS TINYINT);"
            ),
            FailureTestCase(
                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) + CAST(-1 AS TINYINT);"
            ),
            // SMALLINT
            FailureTestCase(
                input = "CAST(${Short.MAX_VALUE} AS SMALLINT) + CAST(1 AS SMALLINT);"
            ),
            FailureTestCase(
                input = "CAST(${Short.MIN_VALUE} AS SMALLINT) + CAST(-1 AS SMALLINT);"
            ),
            // INT
            FailureTestCase(
                input = "CAST(${Integer.MAX_VALUE} AS INT) + CAST(1 AS INT);"
            ),
            FailureTestCase(
                input = "CAST(${Integer.MIN_VALUE} AS INT) + CAST(-1 AS INT);"
            ),
            // BIGINT
            FailureTestCase(
                input = "CAST(${Long.MAX_VALUE} AS BIGINT) + CAST(1 AS BIGINT);"
            ),
            FailureTestCase(
                input = "CAST(${Long.MIN_VALUE} AS BIGINT) + CAST(-1 AS BIGINT);"
            )
        )

        @JvmStatic
        fun minusOverflowTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "CAST(${Byte.MAX_VALUE} AS TINYINT) - CAST(-1 AS TINYINT);"
            ),
            FailureTestCase(
                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) - CAST(1 AS TINYINT);"
            ),
            // SMALLINT
            FailureTestCase(
                input = "CAST(${Short.MAX_VALUE} AS SMALLINT) - CAST(-1 AS SMALLINT);"
            ),
            FailureTestCase(
                input = "CAST(${Short.MIN_VALUE} AS SMALLINT) - CAST(1 AS SMALLINT);"
            ),
            // INT
            FailureTestCase(
                input = "CAST(${Integer.MAX_VALUE} AS INT) - CAST(-1 AS INT);"
            ),
            FailureTestCase(
                input = "CAST(${Integer.MIN_VALUE} AS INT) - CAST(1 AS INT);"
            ),
            // BIGINT
            FailureTestCase(
                input = "CAST(${Long.MAX_VALUE} AS BIGINT) - CAST(-1 AS BIGINT);"
            ),
            FailureTestCase(
                input = "CAST(${Long.MIN_VALUE} AS BIGINT) - CAST(1 AS BIGINT);"
            ),
            // Make sure we parse Integer.MIN_VALUE as an INT rather than BIGINT
            FailureTestCase(
                input = "${Integer.MIN_VALUE} - 1"
            ),
            // Make sure we parse Long.MIN_VALUE as an BIGINT rather than DECIMAL/NUMERIC
            FailureTestCase(
                input = "${Long.MIN_VALUE} - 1"
            )
        )

        @JvmStatic
        fun timesOverflowTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "CAST(${Byte.MAX_VALUE} AS TINYINT) * CAST(2 AS TINYINT);"
            ),
            FailureTestCase(
                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) * CAST(2 AS TINYINT);"
            ),
            // SMALLINT
            FailureTestCase(
                input = "CAST(${Short.MAX_VALUE} AS SMALLINT) * CAST(2 AS SMALLINT);"
            ),
            FailureTestCase(
                input = "CAST(${Short.MIN_VALUE} AS SMALLINT) * CAST(2 AS SMALLINT);"
            ),
            // INT
            FailureTestCase(
                input = "CAST(${Integer.MAX_VALUE} AS INT) * CAST(2 AS INT);"
            ),
            FailureTestCase(
                input = "CAST(${Integer.MIN_VALUE} AS INT) * CAST(2 AS INT);"
            ),
            // BIGINT
            FailureTestCase(
                input = "CAST(${Long.MAX_VALUE} AS BIGINT) * CAST(2 AS BIGINT);"
            ),
            FailureTestCase(
                input = "CAST(${Long.MIN_VALUE} AS BIGINT) * CAST(2 AS BIGINT);"
            )
        )

        @JvmStatic
        fun divideTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) / CAST(-1 AS TINYINT)"
            ),
            // SMALLINT
            FailureTestCase(
                input = "CAST(${Short.MIN_VALUE} AS SMALLINT) / CAST(-1 AS SMALLINT)"
            ),
            // INT
            FailureTestCase(
                input = "CAST(${Integer.MIN_VALUE} AS INT) / CAST(-1 AS INT)"
            ),
            // BIGINT
            FailureTestCase(
                input = "CAST(${Long.MIN_VALUE} AS BIGINT) / CAST(-1 AS BIGINT)"
            )
        )

        @JvmStatic
        fun divideByZeroTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "CAST(1 AS TINYINT) / CAST(0 AS TINYINT)"
            ),
            // SMALLINT
            FailureTestCase(
                input = "CAST(1 AS SMALLINT) / CAST(0 AS SMALLINT)"
            ),
            // INT
            FailureTestCase(
                input = "CAST(1 AS INT) / CAST(0 AS INT)"
            ),
            // BIGINT
            FailureTestCase(
                input = "CAST(1 AS BIGINT) / CAST(0 AS BIGINT)"
            ),
            // DECIMAL
            FailureTestCase(
                input = "CAST(1.0 AS DECIMAL) / CAST(0.0 AS DECIMAL)"
            ),
            // NUMERIC
            FailureTestCase(
                input = "CAST(1.0 AS NUMERIC) / CAST(0.0 AS NUMERIC)"
            ),
            // FLOAT
            FailureTestCase(
                input = "CAST(1.0e0 AS FLOAT) / CAST(0.0 AS FLOAT)"
            ),
            // REAL
            FailureTestCase(
                input = "CAST(1.0e0 AS DOUBLE PRECISION) / CAST(0.0 AS DOUBLE PRECISION)"
            )
        )

        @JvmStatic
        fun modByZeroTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "CAST(1 AS TINYINT) % CAST(0 AS TINYINT)"
            ),
            // SMALLINT
            FailureTestCase(
                input = "CAST(1 AS SMALLINT) % CAST(0 AS SMALLINT)"
            ),
            // INT
            FailureTestCase(
                input = "CAST(1 AS INT) % CAST(0 AS INT)"
            ),
            // BIGINT
            FailureTestCase(
                input = "CAST(1 AS BIGINT) % CAST(0 AS BIGINT)"
            ),
            // DECIMAL
            FailureTestCase(
                input = "CAST(1.0 AS DECIMAL) % CAST(0.0 AS DECIMAL)"
            ),
            // NUMERIC
            FailureTestCase(
                input = "CAST(1.0 AS NUMERIC) % CAST(0.0 AS NUMERIC)"
            ),
            // FLOAT
            FailureTestCase(
                input = "CAST(1.0e0 AS FLOAT) % CAST(0.0 AS FLOAT)"
            ),
            // REAL
            FailureTestCase(
                input = "CAST(1.0e0 AS DOUBLE PRECISION) % CAST(0.0 AS DOUBLE PRECISION)"
            )
        )

        @JvmStatic
        fun absOverflowTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "ABS(CAST(${Byte.MIN_VALUE} AS TINYINT))"
            ),
            // SMALLINT
            FailureTestCase(
                input = "ABS(CAST(${Short.MIN_VALUE} AS SMALLINT))"
            ),
            // INT
            FailureTestCase(
                input = "ABS(CAST(${Integer.MIN_VALUE} AS INT))"
            ),
            // BIGINT
            FailureTestCase(
                input = "ABS(CAST(${Long.MIN_VALUE} AS BIGINT))"
            )
        )

        @JvmStatic
        fun negOverflowTests() = listOf(
            // TINYINT
            FailureTestCase(
                input = "-CAST(${Byte.MIN_VALUE} AS TINYINT)"
            ),
            // SMALLINT
            FailureTestCase(
                input = "-CAST(${Short.MIN_VALUE} AS SMALLINT)"
            ),
            // INT
            FailureTestCase(
                input = "-CAST(${Integer.MIN_VALUE} AS INT)"
            ),
            // BIGINT
            FailureTestCase(
                input = "-CAST(${Long.MIN_VALUE} AS BIGINT)"
            ),
            // No explicit casts
            // Double `-`
            // INT
            FailureTestCase(
                input = "- ${Integer.MIN_VALUE}" // space needed since `--` turns into a comment
            ),
            // BIGINT
            FailureTestCase(
                input = "- ${Long.MIN_VALUE}" // space needed since `--` turns into a comment
            ),
            // Triple `-`
            // INT
            FailureTestCase(
                input = "- - ${Integer.MIN_VALUE}"
            ),
            // BIGINT
            FailureTestCase(
                input = "- - ${Long.MIN_VALUE}"
            )
        )
    }
}
