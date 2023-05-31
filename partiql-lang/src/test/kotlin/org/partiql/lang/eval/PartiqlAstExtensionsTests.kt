package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.util.ArgumentsProviderBase

class PartiqlAstExtensionsTests : EvaluatorTestBase() {
    private val desugarer = VisitorTransformMode.DEFAULT.createVisitorTransform()
    private val parser = PartiQLParserBuilder.standard().build()

    data class StartingSourceLocationTestCase(val query: String, val expectedSourceLocationMeta: SourceLocationMeta)

    class ArgsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // arithmetic ops
            StartingSourceLocationTestCase("1 + 2 - 3 * 4 / 5 % 6 + 7", SourceLocationMeta(1L, 1L, 1L)),
            // arithmetic ops with symbols
            StartingSourceLocationTestCase("a + b + c + d + e % f + g", SourceLocationMeta(1L, 1L, 1L)),
            // logical ops with symbols
            StartingSourceLocationTestCase("foo AND bar OR baz", SourceLocationMeta(1L, 1L, 3L)),
            // string ops with symbols
            StartingSourceLocationTestCase("str1 || str2 LIKE str3", SourceLocationMeta(1L, 1L, 4L)),

            // arithmetic ops over multiple lines
            StartingSourceLocationTestCase(
                """
                1 + 2 - 3 * 4 / 5 % 6 + 7
                """,
                SourceLocationMeta(2L, 17L, 1L)
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun test(tc: StartingSourceLocationTestCase) {
        val statement = parser.parseAstStatement(tc.query)
        val queryExpr = (statement as PartiqlAst.Statement.Query).expr

        val actualSourceLocationMeta = queryExpr.getStartingSourceLocationMeta()
        assertEquals(tc.expectedSourceLocationMeta, actualSourceLocationMeta)
    }

    data class VarsTestCase(val expr: String, val expected: Set<String>)

    // Testing PartiqlAst.Expr.boundVariables()
    @ParameterizedTest
    @ArgumentsSource(BoundVariablesTestCases::class)
    fun testBoundVariables(tc: VarsTestCase) {
        val statement = parser.parseAstStatement(tc.expr)
        val desugared = desugarer.transformStatement(statement)
        val queryExpr = (desugared as PartiqlAst.Statement.Query).expr as PartiqlAst.Expr.Select
        val actual = queryExpr.boundVariables()
        assertEquals(tc.expected, actual)
    }

    class BoundVariablesTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<VarsTestCase> = listOf(
            VarsTestCase("SELECT x AS y FROM [1,2,3] AS x", setOf("x")),
            VarsTestCase("SELECT x AS y FROM [1,2,3]", setOf("_1")), // setOf() without desugaring
            VarsTestCase("SELECT x AS y FROM [1,2,3] AS x AT i", setOf("x", "i")),
            VarsTestCase("SELECT x AS y FROM t AS x", setOf("x")),
            VarsTestCase("SELECT x AS y FROM t", setOf("t")), // setOf() without desugaring

            VarsTestCase("SELECT x  AS y FROM t1 AS x1, t2 x2 LET x1 + x2 AS z where a = b", setOf("x1", "x2", "z")),
            VarsTestCase("SELECT x  AS y FROM t1 AS x1, t2 x2 LET x1 + x2 AS y where a = b", setOf("x1", "x2", "y")),

            VarsTestCase("SELECT x FROM t AS x where x.a = (SELECT max(n) FROM x AS z)", setOf("x")),
            VarsTestCase("SELECT DISTINCT t.a, COUNT(t.b) AS c FROM Tbl t GROUP BY t.a", setOf("t")),
        )
    }

    // Testing PartiqlAst.Expr.freeVariables()

    @ParameterizedTest
    @ArgumentsSource(FreeVariablesTestCases::class)
    fun testFreeVariables(tc: VarsTestCase) {
        val statement = parser.parseAstStatement(tc.expr)
        val queryExpr = (statement as PartiqlAst.Statement.Query).expr
        val actual = queryExpr.freeVariables()
        assertEquals(tc.expected, actual)
    }

    class FreeVariablesTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<VarsTestCase> = listOf(
            VarsTestCase("x", setOf("x")),
            VarsTestCase("x + y", setOf("x", "y")),
            VarsTestCase("SELECT x AS y FROM [1,2,3] AS x", setOf()),
            VarsTestCase("SELECT x AS y FROM t AS x", setOf("t")),
            VarsTestCase("SELECT x   AS y FROM t AS x where a = b", setOf("t", "a", "b")),
            VarsTestCase("SELECT x.g AS y FROM t AS x where a = b.f", setOf("t", "a", "b")),
            VarsTestCase("SELECT t.g AS y FROM t      where a = b.f", setOf("t", "a", "b")),
            VarsTestCase("SELECT   g AS y FROM t      where a = b.f", setOf("g", "t", "a", "b")),

            VarsTestCase("SELECT x        FROM t AS x where a = b", setOf("t", "a", "b")),
            VarsTestCase("SELECT x.g      FROM t AS x where a = b.f", setOf("t", "a", "b")),
            VarsTestCase("SELECT t.g      FROM t      where a = b.f", setOf("t", "a", "b")),
            VarsTestCase("SELECT   g      FROM t      where a = b.f", setOf("g", "t", "a", "b")),

            VarsTestCase("SELECT x FROM t AS x where x.a = (SELECT max(n) FROM x)", setOf("t", "n")),
            VarsTestCase("SELECT DISTINCT t.a, COUNT(t.b) AS c FROM Tbl t GROUP BY t.a", setOf("Tbl")),
            VarsTestCase("SELECT DISTINCT t.a, COUNT(t.b) AS c FROM Tbl t GROUP BY t.a AS z HAVING z = a", setOf("Tbl", "a")),

            VarsTestCase("SELECT t.a as x, t.b as y, s as z  FROM Tbl as t at i", setOf("Tbl", "s")),
            VarsTestCase("SELECT t, x FROM Tbl t, t as x", setOf("Tbl")),

            VarsTestCase("SELECT s, t, x*x, y as z FROM Tbl as t LET t.a + t.b as x, t.a-z as y", setOf("Tbl", "s", "z")),
            VarsTestCase("SELECT 5                 FROM Tbl as t LET t.a + t.b as x, t.a-z as y", setOf("Tbl", "z")),
            VarsTestCase("SELECT s, t, x*x, y as z FROM Tbl as t LET t.a + t.b as x, 2*x   as y", setOf("Tbl", "s")),

            VarsTestCase("        SELECT t, x, y, z FROM t as x, Foo as y", setOf("t", "Foo", "z")),
            VarsTestCase("SELECT (SELECT t, x, y, z FROM t as x, Foo as y) as s FROM Tbl as t", setOf("Tbl", "Foo", "z")),
        )
    }
}
