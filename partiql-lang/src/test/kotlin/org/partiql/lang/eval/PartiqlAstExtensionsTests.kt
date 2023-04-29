package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.PartiQLParser
import org.partiql.lang.util.ArgumentsProviderBase

class PartiqlAstExtensionsTests : EvaluatorTestBase() {
    private val parser = PartiQLParser()

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
}
