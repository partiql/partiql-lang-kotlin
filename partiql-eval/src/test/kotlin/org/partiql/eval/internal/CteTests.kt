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
            // A (non-recursive) WITH list element may reference sibling elements defined earlier in the same
            // WITH list, matching the behavior of Redshift, Trino, and Spark.
            SuccessTestCase(
                name = "WITH list element references an earlier sibling element",
                input = """
                    WITH
                        x AS (SELECT VALUE t FROM << 1, 2, 3 >> t),
                        y AS (SELECT VALUE v FROM x AS v)
                    SELECT VALUE y FROM y;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
            SuccessTestCase(
                name = "WITH list element references an earlier sibling element (used with cross join)",
                input = """
                    WITH
                        x AS (SELECT VALUE t FROM << 1, 2, 3 >> t),
                        y AS (SELECT VALUE v FROM x AS v)
                    SELECT * FROM x, y;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("_1", Datum.integer(1)), Field.of("_2", Datum.integer(1))),
                    Datum.struct(Field.of("_1", Datum.integer(1)), Field.of("_2", Datum.integer(2))),
                    Datum.struct(Field.of("_1", Datum.integer(1)), Field.of("_2", Datum.integer(3))),
                    Datum.struct(Field.of("_1", Datum.integer(2)), Field.of("_2", Datum.integer(1))),
                    Datum.struct(Field.of("_1", Datum.integer(2)), Field.of("_2", Datum.integer(2))),
                    Datum.struct(Field.of("_1", Datum.integer(2)), Field.of("_2", Datum.integer(3))),
                    Datum.struct(Field.of("_1", Datum.integer(3)), Field.of("_2", Datum.integer(1))),
                    Datum.struct(Field.of("_1", Datum.integer(3)), Field.of("_2", Datum.integer(2))),
                    Datum.struct(Field.of("_1", Datum.integer(3)), Field.of("_2", Datum.integer(3)))
                )
            ),
            // Chained references: z references y, which references x.
            SuccessTestCase(
                name = "WITH list elements reference chained earlier siblings",
                input = """
                    WITH
                        x AS (SELECT VALUE t FROM << 1, 2, 3 >> t),
                        y AS (SELECT VALUE v * 10 FROM x AS v),
                        z AS (SELECT VALUE v + 1 FROM y AS v)
                    SELECT VALUE z FROM z;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(11),
                    Datum.integer(21),
                    Datum.integer(31)
                )
            ),
            // Three chained sibling CTEs (y references x, z references y), then join all three.
            SuccessTestCase(
                name = "Chained sibling CTEs joined together",
                input = """
                    WITH
                        x AS (SELECT t.id AS id, t.a AS a FROM << { 'id': 1, 'a': 'a1' }, { 'id': 2, 'a': 'a2' } >> AS t),
                        y AS (SELECT x.id AS id, x.a AS b FROM x),
                        z AS (SELECT y.id AS id, y.b AS c FROM y)
                    SELECT x.a AS a, y.b AS b, z.c AS c
                    FROM x
                        INNER JOIN y ON x.id = y.id
                        INNER JOIN z ON y.id = z.id;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.string("a1")), Field.of("b", Datum.string("a1")), Field.of("c", Datum.string("a1"))),
                    Datum.struct(Field.of("a", Datum.string("a2")), Field.of("b", Datum.string("a2")), Field.of("c", Datum.string("a2")))
                )
            ),
            // Query body joins two CTEs with an explicit INNER JOIN ... ON.
            SuccessTestCase(
                name = "Query joins two WITH elements with INNER JOIN ON",
                input = """
                    WITH
                        x AS (SELECT t.id AS id, t.name AS name FROM << { 'id': 1, 'name': 'a' }, { 'id': 2, 'name': 'b' }, { 'id': 3, 'name': 'c' } >> AS t),
                        y AS (SELECT t.id AS id, t.city AS city FROM << { 'id': 2, 'city': 'nyc' }, { 'id': 3, 'city': 'sf' } >> AS t)
                    SELECT x.name AS name, y.city AS city FROM x INNER JOIN y ON x.id = y.id;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("name", Datum.string("b")), Field.of("city", Datum.string("nyc"))),
                    Datum.struct(Field.of("name", Datum.string("c")), Field.of("city", Datum.string("sf")))
                )
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
            // A WITH list element may only reference siblings defined *earlier* in the same WITH list. A forward
            // reference (referencing a sibling defined later) must fail, matching Redshift, Trino, and Spark.
            FailureTestCase(
                name = "Attempting to make a forward reference to a later with list element",
                input = """
                    WITH
                        x AS (SELECT VALUE v FROM y AS v), -- y is defined later; forward reference is not allowed.
                        y AS (SELECT VALUE t FROM << 1, 2, 3 >> t)
                    SELECT * FROM x;
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

    /**
     * End-to-end verification (issue #1868) that a WITH list element which references an earlier sibling
     * element plans, evaluates, and produces the expected result. The reference is resolved and its
     * sub-plan is transformed before it is consumed by the referencing element.
     */
    @Test
    fun siblingReferenceProducesExpectedResult() {
        val tc = SuccessTestCase(
            name = "WITH list element references and transforms an earlier sibling",
            input = """
                WITH
                    base AS (SELECT VALUE n FROM << 1, 2, 3 >> AS n),
                    doubled AS (SELECT VALUE v * 2 FROM base AS v)
                SELECT VALUE d FROM doubled AS d;
            """.trimIndent(),
            mode = Mode.STRICT(),
            expected = Datum.bagVararg(
                Datum.integer(2),
                Datum.integer(4),
                Datum.integer(6)
            )
        )
        tc.run()
    }
}
