package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * This test file tests recursive Common Table Expressions (WITH RECURSIVE).
 */
class RecursiveCteTests {

    @ParameterizedTest
    @MethodSource("successTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun successTests(tc: SuccessTestCase) = tc.run()

    /**
     * There is no iteration limit (SQL:2023 Part 2, Subclause 7.17, NOTE 322), so a recursion without a fixpoint must
     * be cancellable via [Thread.interrupt].
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `a recursion without a fixpoint is interruptible`() {
        val query = """
            WITH RECURSIVE r(n) AS (
                SELECT 1 FROM << 0 >> AS z
                UNION ALL
                SELECT r.n + 1 FROM r
            )
            SELECT VALUE COUNT(*) FROM r
        """.trimIndent()
        val statement = PartiQLParser.standard().parse(query).statements.single()
        val session = Session.builder().catalog("default").catalogs(Catalog.builder().name("default").build()).build()
        val plan = PartiQLPlanner.standard().plan(statement, session).plan
        val prepared = PartiQLCompiler.standard().prepare(plan, Mode.STRICT())
        var thrown: Throwable? = null
        // The result is lazy, so it is materialized to evaluate the query.
        val worker = thread { thrown = runCatching { DatumMaterialize.materialize(prepared.execute()) }.exceptionOrNull() }
        Thread.sleep(200)
        worker.interrupt()
        worker.join()
        val error = assertIs<PRuntimeException>(thrown, "Expected an interruption, but received: $thrown").error
        assertEquals(PError.INTERRUPTED, error.code())
    }

    companion object {

        private val employees = Global(
            name = "employees",
            value = """
                [
                    { "id": 1, "name": "ceo", "manager": null },
                    { "id": 2, "name": "vp1", "manager": 1 },
                    { "id": 3, "name": "vp2", "manager": 1 },
                    { "id": 4, "name": "dev1", "manager": 2 },
                    { "id": 5, "name": "dev2", "manager": 4 },
                    { "id": 6, "name": "other", "manager": 7 }
                ]
            """
        )

        // 1 -> 2 -> 3 -> 1 is a cycle, and 4 is unreachable from 1.
        private val edges = Global(
            name = "edges",
            value = """
                [
                    { "src": 1, "dst": 2 },
                    { "src": 2, "dst": 3 },
                    { "src": 3, "dst": 1 },
                    { "src": 4, "dst": 1 }
                ]
            """
        )

        // 1 -> 2 -> 3 and 1 -> 3, so 3 is reachable from 1 by two paths.
        private val dag = Global(
            name = "dag",
            value = """
                [
                    { "src": 1, "dst": 2 },
                    { "src": 1, "dst": 3 },
                    { "src": 2, "dst": 3 }
                ]
            """
        )

        private val tree = Global(
            name = "tree",
            value = """
                [
                    {
                        "name": "a",
                        "children": [
                            { "name": "b", "children": [ { "name": "d", "children": [] } ] },
                            { "name": "c", "children": [] }
                        ]
                    }
                ]
            """
        )

        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
                name = "Counting with UNION ALL",
                input = """
                    WITH RECURSIVE cnt(n) AS (
                        SELECT 1 AS n FROM << 0 >> AS z
                        UNION ALL
                        SELECT c.n + 1 AS n FROM cnt AS c WHERE c.n < 5
                    )
                    SELECT VALUE c.n FROM cnt AS c;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3),
                    Datum.integer(4),
                    Datum.integer(5),
                )
            ),
            SuccessTestCase(
                name = "The column list renames the columns of each operand",
                input = """
                    WITH RECURSIVE cnt(n) AS (
                        SELECT 1 AS x FROM << 0 >> AS z
                        UNION ALL
                        SELECT cnt.n + 1 FROM cnt WHERE cnt.n < 3
                    )
                    SELECT * FROM cnt;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("n", Datum.integer(1))),
                    Datum.struct(Field.of("n", Datum.integer(2))),
                    Datum.struct(Field.of("n", Datum.integer(3))),
                )
            ),
            SuccessTestCase(
                name = "Hierarchy traversal with a join",
                input = """
                    WITH RECURSIVE reports(id, name, depth) AS (
                        SELECT e.id, e.name, 0 FROM employees AS e WHERE e.id = 2
                        UNION ALL
                        SELECT e.id, e.name, r.depth + 1 FROM employees AS e INNER JOIN reports AS r ON e.manager = r.id
                    )
                    SELECT r.name AS name, r.depth AS depth FROM reports AS r;
                """.trimIndent(),
                mode = Mode.STRICT(),
                globals = listOf(employees),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("name", Datum.string("vp1")), Field.of("depth", Datum.integer(0))),
                    Datum.struct(Field.of("name", Datum.string("dev1")), Field.of("depth", Datum.integer(1))),
                    Datum.struct(Field.of("name", Datum.string("dev2")), Field.of("depth", Datum.integer(2))),
                )
            ),
            SuccessTestCase(
                name = "Hierarchy traversal with a comma join and a WHERE predicate",
                input = """
                    WITH RECURSIVE reports(id) AS (
                        SELECT e.id FROM employees AS e WHERE e.id = 1
                        UNION ALL
                        SELECT e.id FROM reports AS r, employees AS e WHERE e.manager = r.id
                    )
                    SELECT VALUE r.id FROM reports AS r;
                """.trimIndent(),
                globals = listOf(employees),
                expected = Datum.bagVararg(
                    Datum.bigint(1),
                    Datum.bigint(2),
                    Datum.bigint(3),
                    Datum.bigint(4),
                    Datum.bigint(5),
                )
            ),
            SuccessTestCase(
                name = "UNION DISTINCT terminates on a cyclic graph",
                input = """
                    WITH RECURSIVE reachable(node) AS (
                        SELECT e.src FROM edges AS e WHERE e.src = 1
                        UNION
                        SELECT e.dst FROM edges AS e, reachable AS r WHERE e.src = r.node
                    )
                    SELECT VALUE r.node FROM reachable AS r;
                """.trimIndent(),
                globals = listOf(edges),
                expected = Datum.bagVararg(
                    Datum.bigint(1),
                    Datum.bigint(2),
                    Datum.bigint(3),
                )
            ),
            SuccessTestCase(
                name = "UNION DISTINCT removes duplicates from the seed",
                input = """
                    WITH RECURSIVE t(n) AS (
                        SELECT 1 FROM << 0, 0, 0 >> AS z
                        UNION
                        SELECT t.n + 1 FROM t WHERE t.n < 2
                    )
                    SELECT VALUE t.n FROM t;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                )
            ),
            SuccessTestCase(
                name = "UNION ALL keeps duplicates",
                input = """
                    WITH RECURSIVE t(n) AS (
                        SELECT 1 FROM << 0, 0 >> AS z
                        UNION ALL
                        SELECT t.n + 1 FROM t WHERE t.n < 2
                    )
                    SELECT VALUE t.n FROM t;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(2),
                )
            ),
            SuccessTestCase(
                name = "Multiple non-recursive and recursive operands",
                input = """
                    WITH RECURSIVE t(n) AS (
                        SELECT 1 FROM << 0 >> AS z
                        UNION ALL
                        SELECT 10 FROM << 0 >> AS z
                        UNION ALL
                        SELECT t.n + 1 FROM t WHERE t.n < 2
                        UNION ALL
                        SELECT t.n + 100 FROM t WHERE t.n = 10
                    )
                    SELECT VALUE t.n FROM t;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(10),
                    Datum.integer(2),
                    Datum.integer(110),
                )
            ),
            SuccessTestCase(
                name = "Path accumulation widens the working table type",
                input = """
                    WITH RECURSIVE p(n, path) AS (
                        SELECT 1, '1' FROM << 0 >> AS z
                        UNION ALL
                        SELECT p.n + 1, p.path || '>' || CAST(p.n + 1 AS STRING) FROM p WHERE p.n < 3
                    )
                    SELECT VALUE p.path FROM p;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.string("1"),
                    Datum.string("1>2"),
                    Datum.string("1>2>3"),
                )
            ),
            SuccessTestCase(
                name = "A recursive element references a later non-recursive element",
                input = """
                    WITH RECURSIVE
                        r(n) AS (
                            SELECT b.n FROM base AS b
                            UNION ALL
                            SELECT r.n + 1 FROM r WHERE r.n < 3
                        ),
                        base(n) AS (SELECT 1 FROM << 0 >> AS z)
                    SELECT VALUE r.n FROM r;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3),
                )
            ),
            SuccessTestCase(
                name = "A non-recursive element references a recursive element",
                input = """
                    WITH RECURSIVE
                        r(n) AS (
                            SELECT 1 FROM << 0 >> AS z
                            UNION ALL
                            SELECT r.n + 1 FROM r WHERE r.n < 3
                        ),
                        doubled(m) AS (SELECT r.n * 2 FROM r)
                    SELECT VALUE d.m FROM doubled AS d;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(2),
                    Datum.integer(4),
                    Datum.integer(6),
                )
            ),
            SuccessTestCase(
                name = "A recursive element is referenced twice in the query",
                input = """
                    WITH RECURSIVE r(n) AS (
                        SELECT 1 FROM << 0 >> AS z
                        UNION ALL
                        SELECT r.n + 1 FROM r WHERE r.n < 2
                    )
                    SELECT a.n AS a, b.n AS b FROM r AS a, r AS b;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2))),
                    Datum.struct(Field.of("a", Datum.integer(2)), Field.of("b", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.integer(2)), Field.of("b", Datum.integer(2))),
                )
            ),
            SuccessTestCase(
                name = "A recursive element within a subquery",
                input = """
                    SELECT VALUE (
                        WITH RECURSIVE r(n) AS (
                            SELECT 1 FROM << 0 >> AS z
                            UNION ALL
                            SELECT r.n + 1 FROM r WHERE r.n < 4
                        )
                        SELECT VALUE COUNT(*) FROM r
                    ) FROM << 0 >> AS z;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.bagVararg(Datum.bigint(4)),
                )
            ),
            SuccessTestCase(
                name = "The recursion stops when the recursive operand produces no rows",
                input = """
                    WITH RECURSIVE r(n) AS (
                        SELECT 1 FROM << 0 >> AS z
                        UNION ALL
                        SELECT r.n + 1 FROM r WHERE false
                    )
                    SELECT VALUE r.n FROM r;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                )
            ),
            SuccessTestCase(
                name = "UNION ALL keeps a row for each derivation",
                input = """
                    WITH RECURSIVE t(n) AS (
                        SELECT 1 FROM << 0 >> AS z
                        UNION ALL
                        SELECT e.dst FROM t, dag AS e WHERE e.src = t.n
                    )
                    SELECT VALUE t.n FROM t;
                """.trimIndent(),
                globals = listOf(dag),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.bigint(2),
                    Datum.bigint(3),
                    Datum.bigint(3),
                )
            ),
            SuccessTestCase(
                name = "A recursive element is seeded from a later recursive element",
                input = """
                    WITH RECURSIVE
                        b(m) AS (
                            SELECT a.n * 10 FROM a
                            UNION ALL
                            SELECT b.m + 1 FROM b WHERE b.m < 11
                        ),
                        a(n) AS (
                            SELECT 1 FROM << 0 >> AS z
                            UNION ALL
                            SELECT a.n + 1 FROM a WHERE a.n < 3
                        )
                    SELECT VALUE b.m FROM b;
                """.trimIndent(),
                mode = Mode.STRICT(),
                expected = Datum.bagVararg(
                    Datum.integer(10),
                    Datum.integer(20),
                    Datum.integer(30),
                    Datum.integer(11),
                )
            ),
            // SQL:2023 Part 2, Subclause 7.17, General Rule 2.c.ii: each iteration re-evaluates the recursive operand
            // against all rows so far, so the DISTINCT also eliminates duplicates produced by different iterations.
            SuccessTestCase(
                name = "SELECT DISTINCT in the recursive operand of a UNION ALL",
                input = """
                    WITH RECURSIVE t(n) AS (
                        SELECT 1 FROM << 0 >> AS z
                        UNION ALL
                        SELECT DISTINCT e.dst FROM t, dag AS e WHERE e.src = t.n
                    )
                    SELECT VALUE t.n FROM t;
                """.trimIndent(),
                globals = listOf(dag),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.bigint(2),
                    Datum.bigint(3),
                )
            ),
        )
    }
}
