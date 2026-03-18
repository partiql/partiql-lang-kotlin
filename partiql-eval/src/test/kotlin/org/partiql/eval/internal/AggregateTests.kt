package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

class AggregateTests {

    @ParameterizedTest
    @MethodSource("avgCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testAvg(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("sumCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSum(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun avgCases() = listOf(
            SuccessTestCase(
                name = "AVG of Ion integer literals",
                input = "SELECT AVG(t.v) FROM << {'v': `1`}, {'v': `2`}, {'v': `3`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(2)))
                )
            ),
            SuccessTestCase(
                name = "AVG of PartiQL integer literals",
                input = "SELECT AVG(t.v) FROM << {'v': 1}, {'v': 2}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(2)))
                )
            ),
            SuccessTestCase(
                name = "AVG of mixed integer literals, result is integer",
                input = "SELECT AVG(t.v) FROM << {'v': `1`}, {'v': `2.0`}, {'v': `3e0`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(2)))
                )
            ),
            SuccessTestCase(
                name = "AVG of mixed integer literals, result is double",
                input = "SELECT AVG(t.v) FROM << {'v': `1`}, {'v': `2.3`}, {'v': `3e0`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.doublePrecision(2.1)))
                )
            ),
        )

        @JvmStatic
        fun sumCases() = listOf(
            SuccessTestCase(
                name = "SUM of Ion integer literals",
                input = "SELECT SUM(t.v) FROM << {'v': `1`}, {'v': `2`}, {'v': `3`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(6)))
                )
            ),
            SuccessTestCase(
                name = "SUM of mixed integer literals, result is integer",
                input = "SELECT SUM(t.v) FROM << {'v': `1`}, {'v': `2.0`}, {'v': `3e0`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(6)))
                )
            ),
            SuccessTestCase(
                name = "SUM of mixed integer literals, result is double",
                input = "SELECT SUM(t.v) FROM << {'v': `1`}, {'v': `2.1`}, {'v': `3e0`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.doublePrecision(6.1)))
                )
            ),
            SuccessTestCase(
                name = "SUM of PartiQL integer literals",
                input = "SELECT SUM(t.v) FROM << {'v': 1}, {'v': 2.0}, {'v': 3e0} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(6)))
                )
            ),
        )
    }
}
