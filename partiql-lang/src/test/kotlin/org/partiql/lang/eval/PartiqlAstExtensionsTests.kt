package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.PartiQLParser
import org.partiql.lang.util.ArgumentsProviderBase

class PartiqlAstExtensionsTests : EvaluatorTestBase() {
    private val parser = PartiQLParser()
    private val desugarer = VisitorTransformMode.DEFAULT.createVisitorTransform()

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
}
