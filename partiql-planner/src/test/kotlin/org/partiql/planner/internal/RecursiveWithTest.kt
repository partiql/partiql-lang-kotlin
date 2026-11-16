/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.planner.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.Plan
import org.partiql.plan.rel.RelRecursiveUnion
import org.partiql.plan.rel.RelWith
import org.partiql.plan.rex.RexWorkingTable
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.util.PErrorCollector
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.types.PType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the planning of recursive WITH list elements (recursive common table expressions).
 */
class RecursiveWithTest {

    private val parser = PartiQLParser.standard()

    private val session = Session.builder()
        .catalog("default")
        .catalogs(Catalog.builder().name("default").build())
        .build()

    @Test
    fun `recursive element is planned as a recursive union over its working table`() {
        val query = """
            WITH RECURSIVE cnt(n) AS (
                SELECT 1 AS n FROM << 0 >> AS z
                UNION ALL
                SELECT c.n + 1 AS n FROM cnt AS c WHERE c.n < 5
            )
            SELECT VALUE c.n FROM cnt AS c
        """.trimIndent()
        val (plan, errors) = plan(query)
        assertTrue(errors.problems.isEmpty(), "Unexpected problems: ${errors.problems}")
        // The element is inlined into the query, and it is also retained by the WITH clause.
        val unions = plan.collect<RelRecursiveUnion>()
        assertTrue(unions.isNotEmpty())
        assertEquals(1, unions.map { it.id }.toSet().size)
        val union = unions.first()
        assertEquals("cnt", union.name)
        assertTrue(union.isAll)
        val workingTables = union.recursive.collect<RexWorkingTable>()
        assertEquals(1, workingTables.size)
        assertEquals(union.id, workingTables.single().id)
        assertTrue(union.seed.collect<RexWorkingTable>().isEmpty())
        // The column list determines the row type of the working table.
        val row = workingTables.single().type.pType.typeParameter
        assertEquals(PType.ROW, row.code())
        assertEquals(listOf("n"), row.fields.map { it.name })
        assertEquals(PType.INTEGER, row.fields.single().type.code())
    }

    @Test
    fun `recursive element is planned without inlining`() {
        val query = """
            WITH RECURSIVE cnt(n) AS (
                SELECT 1 FROM << 0 >> AS z
                UNION
                SELECT cnt.n + 1 FROM cnt WHERE cnt.n < 5
            )
            SELECT VALUE c.n FROM cnt AS c
        """.trimIndent()
        val planner = PartiQLPlanner.builder().forceInlineWithClause(false).build()
        val (plan, errors) = plan(query, planner)
        assertTrue(errors.problems.isEmpty(), "Unexpected problems: ${errors.problems}")
        val union = plan.collect<RelRecursiveUnion>().single()
        assertTrue(!union.isAll)
        assertEquals(union.id, union.recursive.collect<RexWorkingTable>().single().id)
    }

    @Test
    fun `the recursive reference may be on the preserved side of an outer join`() {
        val query = """
            WITH RECURSIVE r(n) AS (
                SELECT 1 FROM << 0 >> AS z
                UNION ALL
                SELECT r.n + 1 FROM r LEFT JOIN << 1, 2 >> AS t ON r.n = t WHERE r.n < 3
            )
            SELECT VALUE r.n FROM r
        """.trimIndent()
        val (_, errors) = plan(query)
        assertTrue(errors.problems.isEmpty(), "Unexpected problems: ${errors.problems}")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRecursiveQueries")
    fun `invalid recursive queries are reported`(name: String, query: String, message: String) {
        val (_, errors) = plan(query)
        val problems = errors.errors.filter { it.code() == PError.INVALID_RECURSIVE_QUERY }
        assertTrue(problems.isNotEmpty(), "Expected INVALID_RECURSIVE_QUERY, but received: ${errors.problems}")
        val messages = problems.map { it.getOrNull("MESSAGE", String::class.java)!! }
        assertTrue(messages.any { it.contains(message) }, "Expected a message containing '$message', but received: $messages")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedRecursiveQueries")
    fun `unsupported recursive queries are reported`(name: String, query: String, feature: String) {
        val (_, errors) = plan(query)
        val features = errors.errors
            .filter { it.code() == PError.FEATURE_NOT_SUPPORTED }
            .map { it.getOrNull("FEATURE_NAME", String::class.java)!! }
        assertTrue(features.any { it.contains(feature) }, "Expected FEATURE_NOT_SUPPORTED '$feature', but received: ${errors.problems}")
        assertTrue(errors.errors.none { it.code() == PError.INVALID_RECURSIVE_QUERY }, "Unexpected problems: ${errors.problems}")
    }

    @Test
    fun `the working table type is widened to cover the recursive operand`() {
        val query = "WITH RECURSIVE t(n) AS ($SEED UNION ALL SELECT CAST(t.n + 1 AS BIGINT) FROM t WHERE t.n < 3) SELECT VALUE t.n FROM t"
        assertEquals(PType.BIGINT, workingTableColumn(query).code())
    }

    @Test
    fun `the working table type falls back to dynamic if widening does not converge`() {
        // The precision of the DECIMAL grows with each typing pass.
        val query = "WITH RECURSIVE t(n) AS ($SEED UNION ALL SELECT t.n + 0.5 FROM t WHERE t.n < 2) SELECT VALUE t.n FROM t"
        assertEquals(PType.DYNAMIC, workingTableColumn(query).code())
    }

    @Test
    fun `a regular query name matches a delimited element name case-insensitively`() {
        val query = """
            WITH RECURSIVE "Cnt"(n) AS ($SEED UNION ALL SELECT c.n + 1 FROM cnt AS c WHERE c.n < 3)
            SELECT VALUE c.n FROM "Cnt" AS c
        """.trimIndent()
        val (plan, errors) = plan(query)
        assertTrue(errors.problems.isEmpty(), "Unexpected problems: ${errors.problems}")
        assertTrue(plan.collect<RexWorkingTable>().isNotEmpty())
    }

    @Test
    fun `an invalid element is reported once`() {
        val query = "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x GROUP BY x.n) SELECT * FROM x"
        val (_, errors) = plan(query)
        assertEquals(1, errors.problems.size, "Received: ${errors.problems}")
    }

    /**
     * Returns the type of the single column of the working table of the single recursive element of [query].
     */
    private fun workingTableColumn(query: String): PType {
        val (plan, errors) = plan(query)
        assertTrue(errors.errors.isEmpty(), "Unexpected problems: ${errors.problems}")
        val row = plan.collect<RexWorkingTable>().first().type.pType.typeParameter
        return when (row.code()) {
            PType.ROW -> row.fields.single().type
            else -> row
        }
    }

    private fun plan(query: String, planner: PartiQLPlanner = PartiQLPlanner.standard()): Pair<Plan, PErrorCollector> {
        val statement = parser.parse(query).statements.single()
        val errors = PErrorCollector()
        val result = planner.plan(statement, session, Context.of(errors))
        return result.plan to errors
    }

    private inline fun <reified T : Operator> Plan.collect(): List<T> = (action as Action.Query).rex.collect()

    private inline fun <reified T : Operator> Operator.collect(): List<T> = descendants().filterIsInstance<T>()

    // WITH list elements are not operands of a [RelWith], so they are visited explicitly.
    private fun Operator.descendants(): List<Operator> {
        val children = operands.flatMap { operand -> operand.toList() }
        val elements = (this as? RelWith)?.elements?.map { it.representation } ?: emptyList()
        return listOf(this) + (children + elements).flatMap { it.descendants() }
    }

    companion object {

        private const val SEED = "SELECT 1 FROM << 0 >> AS z"

        private fun case(name: String, query: String, message: String) = Arguments.of(name, query, message)

        /**
         * Queries violating the syntax rules of SQL:2023 Part 2, Subclause 7.17.
         */
        @JvmStatic
        fun invalidRecursiveQueries() = listOf(
            // Syntax Rule 3.j.i
            case(
                "missing column list",
                "WITH RECURSIVE x AS ($SEED UNION ALL SELECT x.n FROM x) SELECT * FROM x",
                "a column list is required",
            ),
            case(
                "missing column list of a non-recursive element",
                "WITH RECURSIVE y AS ($SEED) SELECT * FROM y",
                "a column list is required",
            ),
            // Syntax Rule 3.c
            case(
                "duplicate query names",
                "WITH RECURSIVE x(n) AS ($SEED), x(m) AS ($SEED) SELECT * FROM x",
                "the query name is defined more than once",
            ),
            case(
                "duplicate query names that differ in case",
                "WITH RECURSIVE x(n) AS ($SEED), X(m) AS ($SEED) SELECT * FROM x",
                "the query name is defined more than once",
            ),
            // Syntax Rule 3.d.ii
            case(
                "column list with duplicate names",
                "WITH RECURSIVE x(n, n) AS (SELECT 1, 2 FROM << 0 >> AS z UNION ALL SELECT x.n, x.n FROM x) SELECT * FROM x",
                "duplicate names",
            ),
            case(
                "column list with too many columns",
                "WITH RECURSIVE x(n, m) AS ($SEED UNION ALL SELECT x.n FROM x) SELECT * FROM x",
                "the column list has 2 column(s), but the query produces 1",
            ),
            case(
                "recursive operand with too many columns",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n, x.n FROM x) SELECT * FROM x",
                "the column list has 1 column(s), but the query produces 2",
            ),
            // Syntax Rule 3.h
            case(
                "nested WITH RECURSIVE",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x WHERE x.n < (WITH RECURSIVE y(k) AS ($SEED UNION ALL SELECT y.k FROM y WHERE false) SELECT VALUE COUNT(*) FROM y)) SELECT * FROM x",
                "must not contain another WITH RECURSIVE clause",
            ),
            // Syntax Rule 3.j.vii
            case(
                "not a UNION",
                "WITH RECURSIVE x(n) AS (SELECT x.n FROM x) SELECT * FROM x",
                "must be of the form",
            ),
            case(
                "no non-recursive operand",
                "WITH RECURSIVE x(n) AS (SELECT a.n FROM x AS a UNION ALL SELECT b.n FROM x AS b) SELECT * FROM x",
                "must be of the form",
            ),
            // Syntax Rules 3.j.ix.2 and 3.j.x
            case(
                "non-linear recursion",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT a.n FROM x AS a, x AS b) SELECT * FROM x",
                "linear recursion",
            ),
            case(
                "second recursive reference outside the FROM clause",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT r.n FROM << 0 >> AS q, x AS r WHERE EXISTS(x)) SELECT * FROM x",
                "at most once in a recursive operand",
            ),
            // Syntax Rules 3.j.ix.3 and 3.j.ix.4
            case(
                "LIMIT on the recursive element",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x LIMIT 1) SELECT * FROM x",
                "LIMIT is not allowed",
            ),
            case(
                "LIMIT in the recursive operand",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL (SELECT x.n FROM x LIMIT 1)) SELECT * FROM x",
                "LIMIT",
            ),
            // Syntax Rule 3.j.ix.5.B
            case(
                "recursive reference in a subquery",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT t AS n FROM << 1 >> AS t WHERE t IN (SELECT VALUE x.n FROM x)) SELECT * FROM x",
                "within a subquery",
            ),
            case(
                "recursive reference in a subquery of a derived table",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT d.n FROM (SELECT t AS n FROM << 1 >> AS t WHERE t IN (SELECT VALUE x.n FROM x)) AS d) SELECT * FROM x",
                "within a subquery",
            ),
            // Syntax Rule 3.j.ix.5.C
            case(
                "aggregate function in the recursive operand",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT COUNT(*) AS n FROM x) SELECT * FROM x",
                "aggregate or window functions",
            ),
            case(
                "aggregate function in HAVING of the recursive operand",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x GROUP BY x.n HAVING COUNT(*) > 1) SELECT * FROM x",
                "aggregate or window functions",
            ),
            case(
                "window function in the recursive operand",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT ROW_NUMBER() OVER (ORDER BY x.n) AS n FROM x) SELECT * FROM x",
                "aggregate or window functions",
            ),
            // Syntax Rules 3.j.ix.5.D and 3.j.ix.5.E
            case(
                "recursive reference within EXCEPT ALL",
                "WITH RECURSIVE x(n) AS (($SEED UNION ALL SELECT x.n FROM x) EXCEPT ALL $SEED) SELECT * FROM x",
                "INTERSECT ALL or EXCEPT ALL",
            ),
            case(
                "recursive reference in the right operand of EXCEPT",
                "WITH RECURSIVE x(n) AS (($SEED UNION ALL SELECT x.n FROM x) EXCEPT SELECT x.n FROM x) SELECT * FROM x",
                "right operand of EXCEPT",
            ),
            // Syntax Rules 3.j.ix.5.F and 3.j.ix.5.G
            case(
                "recursive reference on the null-producing side of a LEFT JOIN",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM << 1 >> AS t LEFT JOIN x ON t = x.n) SELECT * FROM x",
                "null-producing side",
            ),
            case(
                "recursive reference on the null-producing side of a RIGHT JOIN",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x RIGHT JOIN << 1 >> AS t ON t = x.n) SELECT * FROM x",
                "null-producing side",
            ),
            case(
                "recursive reference on the null-producing side of a FULL JOIN",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x FULL JOIN << 1 >> AS t ON t = x.n) SELECT * FROM x",
                "null-producing side",
            ),
        )

        /**
         * Valid queries (per SQL:2023 Part 2, Subclause 7.17) that use features which are not supported.
         */
        @JvmStatic
        fun unsupportedRecursiveQueries() = listOf(
            case(
                "mutually recursive elements",
                "WITH RECURSIVE a(n) AS ($SEED UNION ALL SELECT b.n FROM b), b(n) AS (SELECT a.n FROM a) SELECT VALUE a.n FROM a",
                "mutually recursive WITH list elements",
            ),
            case(
                "ORDER BY on the recursive element",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x ORDER BY n) SELECT * FROM x",
                "ORDER BY on a recursive WITH list element",
            ),
            case(
                "OFFSET on the recursive element",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x OFFSET 1) SELECT * FROM x",
                "OFFSET on a recursive WITH list element",
            ),
            case(
                "ORDER BY on a recursive operand",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL (SELECT x.n FROM x ORDER BY x.n)) SELECT * FROM x",
                "ORDER BY on a recursive operand",
            ),
            case(
                "WITH clause within the recursive element",
                "WITH RECURSIVE x(n) AS (WITH y AS ($SEED) SELECT y._1 FROM y UNION ALL SELECT x.n FROM x) SELECT * FROM x",
                "WITH clause within a recursive WITH list element",
            ),
            case(
                "OUTER UNION",
                "WITH RECURSIVE x(n) AS ($SEED OUTER UNION ALL SELECT x.n FROM x) SELECT * FROM x",
                "top-level set operator is not UNION",
            ),
            case(
                "top-level EXCEPT",
                "WITH RECURSIVE x(n) AS (($SEED UNION ALL SELECT x.n FROM x WHERE x.n < 3) EXCEPT SELECT 2 FROM << 0 >> AS z) SELECT * FROM x",
                "top-level set operator is not UNION",
            ),
            case(
                "UNION ALL and UNION DISTINCT",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x UNION SELECT x.n + 1 FROM x) SELECT * FROM x",
                "non-recursive operand is within a nested set operation",
            ),
            case(
                "recursive operand that is a set operation",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL (SELECT x.n FROM x UNION ALL SELECT x.n + 1 FROM x)) SELECT * FROM x",
                "recursive operand that is not a SELECT query",
            ),
            case(
                "recursive reference in a derived table",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT d.n FROM (SELECT x.n FROM x) AS d) SELECT * FROM x",
                "recursive reference within a derived table",
            ),
            // An inlined WITH list element is re-typed where it is referenced; see WithInliningTest.
            case(
                "recursive element referencing a variable of an enclosing query",
                "SELECT VALUE (WITH RECURSIVE r(n) AS (SELECT o.lo FROM << 0 >> AS z UNION ALL SELECT r.n + 1 FROM r WHERE r.n < o.hi) SELECT VALUE r.n FROM r) FROM << { 'lo': 1, 'hi': 3 } >> AS o",
                "WITH list element referencing a variable of an enclosing query",
            ),
            case(
                "recursive element containing a lateral join",
                "WITH RECURSIVE nodes(name, children) AS (SELECT t.name, t.children FROM << { 'name': 'a', 'children': [] } >> AS t UNION ALL SELECT c.name, c.children FROM nodes AS p, p.children AS c) SELECT VALUE n.name FROM nodes AS n",
                "WITH list element containing a lateral (correlated) join",
            ),
            case(
                "path with wildcard steps over the recursive reference",
                "WITH RECURSIVE x(n, k) AS (SELECT 1, [] FROM << 0 >> AS z UNION ALL SELECT c.n, c.k FROM x[*].k[*] AS c) SELECT * FROM x",
                "path with wildcard steps over a recursive reference",
            ),
            case(
                "GROUP BY without aggregate functions in the recursive operand",
                "WITH RECURSIVE x(n) AS ($SEED UNION ALL SELECT x.n FROM x GROUP BY x.n) SELECT * FROM x",
                "GROUP BY, HAVING, or WINDOW clause in a recursive operand",
            ),
            case(
                "column list for rows whose columns are not statically known",
                "WITH RECURSIVE x(n) AS (SELECT VALUE 1 FROM << 0 >> AS z UNION ALL SELECT VALUE x FROM x) SELECT * FROM x",
                "query whose columns are not statically known",
            ),
            case(
                "column list for a query producing duplicate column names",
                "WITH RECURSIVE x(a, b) AS (SELECT 1 AS n, 2 AS n FROM << 0 >> AS z UNION ALL SELECT x.a, x.b FROM x) SELECT * FROM x",
                "query producing duplicate column names",
            ),
        )
    }
}
