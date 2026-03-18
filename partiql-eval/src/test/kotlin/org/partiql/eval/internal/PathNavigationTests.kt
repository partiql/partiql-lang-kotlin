package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

class PathNavigationTests {

    @ParameterizedTest
    @MethodSource("coalesceCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCoalesce(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("nullIfCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testNullIf(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun coalesceCases() = listOf(
            // COALESCE with non-existent path — t.a.b.c does not exist, result column is missing
            SuccessTestCase(
                name = "COALESCE with non-existent nested path",
                input = "SELECT COALESCE(t.a.b.c, 'fallback') AS result FROM << {'a': {'x': 1}} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("result", Datum.string("fallback")))
                )
            ),
            // COALESCE with valid path — t.a.x resolves to 1
            SuccessTestCase(
                name = "COALESCE with valid nested path",
                input = "SELECT COALESCE(t.a.x, 'fallback') AS result FROM [{'a': {'x': 1}}, {'a': {'x': NULL}}] AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("result", Datum.integer(1))),
                    Datum.struct(Field.of("result", Datum.string("fallback")))
                )
            ),
            // COALESCE with valid fallback path
            SuccessTestCase(
                name = "COALESCE with non-existent nested path",
                input = "SELECT COALESCE(t.a.x, t.a.y) AS result FROM  [{'a': {'x': 1, 'y': 'str'}}, {'a': {'x': NULL, 'y': 'str'}}] AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("result", Datum.integer(1))),
                    Datum.struct(Field.of("result", Datum.string("str")))
                )
            ),
            // COALESCE with existing fallback path
            SuccessTestCase(
                name = "COALESCE with non-existent nested path",
                input = "SELECT COALESCE(t.a.x, t.a.z) AS result FROM  [{'a': {'x': 1, 'y': 'str'}}, {'a': {'x': NULL, 'y': 'str'}}, {'a': {'x': MISSING, 'y': 'str'}}] AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("result", Datum.integer(1))),
                    Datum.struct(Field.of("result", Datum.nullValue())),
                    Datum.struct(Field.of("result", Datum.nullValue())),
                )
            ),
        )

        @JvmStatic
        fun nullIfCases() = listOf(
            // NULLIF with non-existent path — t.a.b.c does not exist, result column is missing
            SuccessTestCase(
                name = "NULLIF with non-existent nested path",
                input = "SELECT NULLIF(t.a.b.c, '-1') AS result FROM << {'a': {'x': 1}}, {'a': {'x': -1}} >> AS t",
                expected = Datum.bagVararg(
                    Datum.struct(),
                    Datum.struct()
                )
            ),
            // NULLIF with valid path — t.a.x resolves; string '-1' does not match integer so values pass through
            SuccessTestCase(
                name = "NULLIF with valid nested path",
                input = "SELECT NULLIF(t.a.x, -1) AS result FROM [{'a': {'x': 1}}, {'a': {'x': -1}}] AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("result", Datum.integer(1))),
                    Datum.struct(Field.of("result", Datum.nullValue()))
                )
            ),
            // NULLIF with a valid nullifier path
            SuccessTestCase(
                name = "NULLIF with valid nullifier path",
                input = "SELECT NULLIF(t.a.x, t.a.y) AS result FROM  [{'a': {'x': 1, 'y': 1}}, {'a': {'x': -1, 'y': 1}}] AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("result", Datum.nullValue())),
                    Datum.struct(Field.of("result", Datum.integer(-1)))
                )
            ),
            // NULLIF with a non-existent nullifier path
            SuccessTestCase(
                name = "NULLIF with non-existent nullifier path",
                input = "SELECT NULLIF(t.a.x, t.a.z) AS result FROM  [{'a': {'x': 1, 'y': 'str'}}, {'a': {'x': NULL, 'y': 'str'}}, {'a': {'x': MISSING, 'y': 'str'}}] AS t",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("result", Datum.integer(1))),
                    Datum.struct(),
                    Datum.struct(),
                )
            ),
        )
    }
}
