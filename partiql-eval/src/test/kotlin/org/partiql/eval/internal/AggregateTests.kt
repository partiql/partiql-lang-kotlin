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

    @ParameterizedTest
    @MethodSource("minCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testMin(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("maxCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testMax(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("countCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCount(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("anySomeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testAnySome(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("everyCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testEvery(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("distinctCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testDistinct(tc: SuccessTestCase) = tc.run()

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
            SuccessTestCase(
                name = "AVG of mixed PartiQL and Ion integer literals",
                input = "SELECT AVG(t.v) FROM << {'v': 1}, {'v': `2`}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(2)))
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
            SuccessTestCase(
                name = "SUM of mixed PartiQL and Ion integer literals",
                input = "SELECT SUM(t.v) FROM << {'v': 1}, {'v': `2`}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(6)))
                )
            ),
        )

        @JvmStatic
        fun minCases() = listOf(
            SuccessTestCase(
                name = "MIN of Ion integer literals",
                input = "SELECT MIN(t.v) FROM << {'v': `3`}, {'v': `1`}, {'v': `2`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(1)))
                )
            ),
            SuccessTestCase(
                name = "MIN of PartiQL integer literals",
                input = "SELECT MIN(t.v) FROM << {'v': 3}, {'v': 1}, {'v': 2} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(1)))
                )
            ),
            SuccessTestCase(
                name = "MIN of mixed PartiQL and Ion integer literals",
                input = "SELECT MIN(t.v) FROM << {'v': 3}, {'v': `1`}, {'v': 2} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(1)))
                )
            ),
        )

        @JvmStatic
        fun maxCases() = listOf(
            SuccessTestCase(
                name = "MAX of Ion integer literals",
                input = "SELECT MAX(t.v) FROM << {'v': `1`}, {'v': `3`}, {'v': `2`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(3)))
                )
            ),
            SuccessTestCase(
                name = "MAX of PartiQL integer literals",
                input = "SELECT MAX(t.v) FROM << {'v': 1}, {'v': 3}, {'v': 2} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(3)))
                )
            ),
            SuccessTestCase(
                name = "MAX of mixed PartiQL and Ion integer literals",
                input = "SELECT MAX(t.v) FROM << {'v': 1}, {'v': `3`}, {'v': 2} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(3)))
                )
            ),
        )

        @JvmStatic
        fun countCases() = listOf(
            SuccessTestCase(
                name = "COUNT of Ion integer literals",
                input = "SELECT COUNT(t.v) FROM << {'v': `1`}, {'v': `2`}, {'v': `3`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bigint(3)))
                )
            ),
            SuccessTestCase(
                name = "COUNT of PartiQL integer literals",
                input = "SELECT COUNT(t.v) FROM << {'v': 1}, {'v': 2}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bigint(3)))
                )
            ),
            SuccessTestCase(
                name = "COUNT of mixed PartiQL and Ion integer literals",
                input = "SELECT COUNT(t.v) FROM << {'v': 1}, {'v': `2`}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bigint(3)))
                )
            ),
            SuccessTestCase(
                name = "COUNT with nulls excluded",
                input = "SELECT COUNT(t.v) FROM << {'v': 1}, {'v': NULL}, {'v': `3`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bigint(2)))
                )
            ),
        )

        @JvmStatic
        fun anySomeCases() = listOf(
            SuccessTestCase(
                name = "ANY with all true",
                input = "SELECT ANY(t.v) FROM << {'v': true}, {'v': true} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bool(true)))
                )
            ),
            SuccessTestCase(
                name = "ANY with one true",
                input = "SELECT ANY(t.v) FROM << {'v': false}, {'v': true} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bool(true)))
                )
            ),
            SuccessTestCase(
                name = "ANY with all false",
                input = "SELECT ANY(t.v) FROM << {'v': false}, {'v': false} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bool(false)))
                )
            ),
            SuccessTestCase(
                name = "SOME with one true",
                input = "SELECT SOME(t.v) FROM << {'v': false}, {'v': true} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bool(true)))
                )
            ),
        )

        @JvmStatic
        fun everyCases() = listOf(
            SuccessTestCase(
                name = "EVERY with all true",
                input = "SELECT EVERY(t.v) FROM << {'v': true}, {'v': true} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bool(true)))
                )
            ),
            SuccessTestCase(
                name = "EVERY with one false",
                input = "SELECT EVERY(t.v) FROM << {'v': true}, {'v': false} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bool(false)))
                )
            ),
            SuccessTestCase(
                name = "EVERY with all false",
                input = "SELECT EVERY(t.v) FROM << {'v': false}, {'v': false} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bool(false)))
                )
            ),
        )

        @JvmStatic
        fun distinctCases() = listOf(
            SuccessTestCase(
                name = "SUM DISTINCT of Ion integer literals with duplicates",
                input = "SELECT SUM(DISTINCT t.v) FROM << {'v': `1`}, {'v': `1`}, {'v': `3`} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(4)))
                )
            ),
            SuccessTestCase(
                name = "SUM DISTINCT of mixed PartiQL and Ion integer literals",
                input = "SELECT SUM(DISTINCT t.v) FROM << {'v': 1}, {'v': `1`}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(4)))
                )
            ),
            SuccessTestCase(
                name = "AVG DISTINCT of PartiQL integer literals with duplicates",
                input = "SELECT AVG(DISTINCT t.v) FROM << {'v': 1}, {'v': 1}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(2)))
                )
            ),
            SuccessTestCase(
                name = "COUNT DISTINCT of mixed PartiQL and Ion integer literals",
                input = "SELECT COUNT(DISTINCT t.v) FROM << {'v': 1}, {'v': `1`}, {'v': 3} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.bigint(2)))
                )
            ),
        )
    }
}
