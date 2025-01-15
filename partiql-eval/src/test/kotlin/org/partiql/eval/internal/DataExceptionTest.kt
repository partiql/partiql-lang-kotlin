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

    companion object {
        @JvmStatic
        fun plusOverflowTests() = listOf(
            // TINYINT
            // TODO add parsing and planning support for TINYINT
//            FailureTestCase(
//                input = "CAST(${Byte.MAX_VALUE} AS TINYINT) + CAST(1 AS TINYINT);"
//            ),
//            FailureTestCase(
//                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) + CAST(-1 AS TINYINT);"
//            ),
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
            // TODO add parsing and planning support for TINYINT
//            FailureTestCase(
//                input = "CAST(${Byte.MAX_VALUE} AS TINYINT) - CAST(-1 AS TINYINT);"
//            ),
//            FailureTestCase(
//                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) - CAST(1 AS TINYINT);"
//            ),
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
            )
        )

        @JvmStatic
        fun timesOverflowTests() = listOf(
            // TINYINT
            // TODO add parsing and planning support for TINYINT
//            FailureTestCase(
//                input = "CAST(${Byte.MAX_VALUE} AS TINYINT) * CAST(2 AS TINYINT);"
//            ),
//            FailureTestCase(
//                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) * CAST(2 AS TINYINT);"
//            ),
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
            // TODO add parsing and planning support for TINYINT
//            FailureTestCase(
//                input = "CAST(${Byte.MIN_VALUE} AS TINYINT) / CAST(-1 AS TINYINT)"
//            ),
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
            // TODO add parsing and planning support for TINYINT
//            FailureTestCase(
//                input = "CAST(1 AS TINYINT) / CAST(0 AS TINYINT)"
//            ),
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
            )
        )
    }
}
