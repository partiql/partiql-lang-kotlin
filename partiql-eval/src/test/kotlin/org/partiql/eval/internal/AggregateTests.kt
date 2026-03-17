package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

class AggregateTests {

    @ParameterizedTest
    @MethodSource("aggregateCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testAggregates(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun aggregateCases() = listOf(
            SuccessTestCase(
                name = "AVG of Ion integer literals",
                input = "SELECT AVG(t.v) FROM << {'v': `1`}, {'v': `2`}, {'v': `3`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(2)))
                )
            ),
            SuccessTestCase(
                name = "SUM of Ion integer literals",
                input = "SELECT SUM(t.v) FROM << {'v': `1`}, {'v': `2`}, {'v': `3`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(6)))
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
                name = "SUM of PartiQL integer literals",
                input = "SELECT SUM(t.v) FROM << {'v': 1}, {'v': 2}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(6)))
                )
            ),
        )
    }
}
