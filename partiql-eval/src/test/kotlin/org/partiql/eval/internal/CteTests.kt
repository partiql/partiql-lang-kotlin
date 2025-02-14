package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

/**
 * This test file tests Common Table Expressions.
 */
class CteTests {

    @ParameterizedTest
    @MethodSource("successTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun successTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("failureTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun failureTests(tc: FailureTestCase) = tc.run()

    companion object {
        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
                name = "Simple SFW",
                input = """
                    WITH x AS (SELECT VALUE t FROM <<1, 2, 3>> AS t) SELECT VALUE x FROM x;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
            SuccessTestCase(
                name = "Multiple WITH elements and a UNION",
                input = """
                    WITH
                        x AS (SELECT VALUE t FROM <<1, 2, 3>> AS t),
                        y AS (SELECT VALUE t FROM <<4, 5, 6>> AS t),
                        z AS (SELECT VALUE t FROM <<7, 8, 9>> AS t)
                    SELECT VALUE x FROM x UNION SELECT VALUE y FROM y UNION SELECT VALUE z FROM z;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3),
                    Datum.integer(4),
                    Datum.integer(5),
                    Datum.integer(6),
                    Datum.integer(7),
                    Datum.integer(8),
                    Datum.integer(9)
                )
            ),
            SuccessTestCase(
                name = "Simple SFW with repetitive cross join",
                input = """
                    WITH x AS (SELECT VALUE t FROM <<1>> AS t) SELECT * FROM x AS s, x;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("_1", Datum.integer(1)),
                        Field.of("_2", Datum.integer(1))
                    )
                )
            ),
            SuccessTestCase(
                name = "Multiple WITH elements and cross join",
                input = """
                    WITH
                        x AS (SELECT VALUE t FROM <<1>> AS t),
                        y AS (SELECT VALUE t FROM <<2, 3>> AS t)
                    SELECT * FROM x, y;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("_1", Datum.integer(1)),
                        Field.of("_2", Datum.integer(2))
                    ),
                    Datum.struct(
                        Field.of("_1", Datum.integer(1)),
                        Field.of("_2", Datum.integer(3))
                    )
                )
            ),
            SuccessTestCase(
                name = "Nested WITH",
                input = """
                    WITH x AS (
                        WITH y AS (
                            SELECT VALUE t FROM <<1, 2, 3>> AS t
                        ) SELECT VALUE v * 10 FROM y AS v
                    ) SELECT VALUE x + 5 FROM x;

                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(15),
                    Datum.integer(25),
                    Datum.integer(35)
                )
            ),
            SuccessTestCase(
                name = "Handling of subqueries",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM <<1>> AS t
                    )
                    SELECT VALUE y + (SELECT * FROM x) FROM <<100>> AS y;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(Datum.integer(101))
            ),
            SuccessTestCase(
                name = "Handling of subqueries with tuples",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM << { 'a': 1 }>> AS t
                    )
                    SELECT VALUE y + (SELECT * FROM x) FROM <<100>> AS y;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(Datum.integer(101))
            ),
            SuccessTestCase(
                name = "Handling of subqueries with tuples and explicit attribute",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM << { 'a': 1, 'b': 2 }>> AS t
                    )
                    SELECT VALUE y + (SELECT x.a FROM x) FROM <<100>> AS y;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(Datum.integer(101))
            ),
            SuccessTestCase(
                name = "Handling of subqueries with WHERE",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM <<1, 2, 3, 4, 5>> AS t
                    )
                    SELECT VALUE y + (SELECT * FROM x WHERE x > 4) FROM <<100>> AS y;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(Datum.integer(105))
            ),
        )

        @JvmStatic
        fun failureTestCases() = listOf(
            FailureTestCase(
                name = "CTE with cardinality greater than 1 used in subquery",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM <<1, 2>> AS t
                    )
                    SELECT VALUE y + (SELECT * FROM x) FROM <<100>> AS y;
                """.trimIndent(),
            ),
            FailureTestCase(
                name = "Attempting to reference variable outside the with-list-element",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM <<1, 2>> AS t
                    )
                    SELECT * FROM t; -- t should not able to be referenced.
                """.trimIndent(),
            ),
            FailureTestCase(
                name = "Attempting to reference variable from within the with-list-element",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM t -- t should not able to be referenced.
                    )
                    SELECT * FROM << 1, 2, 3>> AS t, x
                """.trimIndent(),
            ),
            // TODO: Figure out if this should be allowed. In PostgreSQL, it is allowed. In SQL Spec, I'm not sure.
            //  As such, updating the implementation to allow for this would be a non-breaking change.
            FailureTestCase(
                name = "Attempting to reference another with list element",
                input = """
                    WITH
                        x AS (SELECT VALUE t FROM << 1, 2, 3 >> t),
                        y AS (SELECT VALUE x FROM x) -- x should not be able to be referenced.
                    SELECT * FROM y;
                """.trimIndent(),
            ),
            FailureTestCase(
                name = "Attempting to reference another with list element (2)",
                input = """
                    WITH
                        x AS (SELECT VALUE t FROM << 1, 2, 3 >> t),
                        y AS (SELECT VALUE x FROM x)
                    SELECT * FROM x, y; -- x & y should not be able to be referenced
                """.trimIndent(),
            ),
            FailureTestCase(
                name = "Attempting to create a recursive (non-labeled) CTE",
                input = """
                    WITH x AS (
                        SELECT VALUE t FROM t -- t should not able to be referenced.
                    )
                    SELECT * FROM << 1, 2, 3>> AS t, x
                """.trimIndent(),
            ),
        )
    }

    // TODO: Figure out the right behavior here.
    @Test
    @Disabled(
        """
        This _maybe_ should fail, since CTE "y" references a non-existing variable "s". In the specification, it is a bit
        vague about what to do in this scenario. Currently, due to https://partiql.org/partiql-lang/#sec:schema-in-tuple-path,
        the implementation does not throw an error at compile-time. It is only during the evaluation of a non-existent
        variable that it throws an error. Therefore, even though we are emitting a warning when compiling the reference to
        "s", it is never used at runtime (and therefore an error is never emitted).
        """
    )
    fun nonReferencedBadCTE() {
        val tc = FailureTestCase(
            name = "Attempting to reference another with list element (3)",
            input = """
                WITH
                    x AS (SELECT VALUE t FROM << 1, 2, 3 >> t),
                    y AS (SELECT VALUE s FROM s) -- this is rubbish!
                SELECT * FROM x;
            """.trimIndent(),
            mode = Mode.STRICT(),
        )
        tc.run()
    }
}
