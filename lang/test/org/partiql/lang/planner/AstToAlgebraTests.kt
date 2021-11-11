package org.partiql.lang.planner

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlAlgebra
import org.partiql.lang.domains.id
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase


class AstToAlgebraTests {

    private fun parseToAlgebra(sql: String): PartiqlAlgebra.Statement {
        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)

        // For now we will elide `basicVisitorTransforms`, but may need to include them later if we find having to
        // specify normalized SQL in our test cases to be too onerous for some reason.

        return astToAlgebra(parser.parseAstStatement(sql))
    }

    data class TestCase(val sql: String, val expectedAlgebra: PartiqlAlgebra.Statement)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForAstToAlgebraTests::class)
    fun testAstToLogicalAlgebra(tc: TestCase) {
        val algebra = assertDoesNotThrow("Parsing TestCase.sql should not throw") {
            parseToAlgebra(tc.sql)
        }
        assertEquals(tc.expectedAlgebra, algebra)
    }

    class ArgumentsForAstToAlgebraTests : ArgumentsProviderBase() {
        // Note that: test cases for SELECT * are not needed since we already have an AST transform which converts
        // it to SELECT f.*, b.*, etc.

        override fun getParameters() = listOf(
            // Basic SELECT VALUE with all three bindings specified
            TestCase(
                "SELECT VALUE foo FROM bar AS b AT c BY d",
                PartiqlAlgebra.build {
                    query(
                        mapValues(
                            id("foo"),
                            bindingsTerm(scan(id("bar"), varDecl("b"), varDecl("c"), varDecl("d")))
                        )
                    )
                }
            ),
            // Basic SELECT VALUE with PATH expression and WHERE clause
            TestCase(
                "SELECT VALUE x.y FROM z AS x WHERE x.y = 42",
                PartiqlAlgebra.build {
                    query(
                        mapValues(
                            path(id("x"), pathExpr(lit(ionString("y")), caseInsensitive())),
                            bindingsTerm(
                                filter(
                                    eq(
                                        path(id("x"), pathExpr(lit(ionString("y")), caseInsensitive())),
                                        lit(ionInt(42))
                                    ),
                                    bindingsTerm(scan(id("z"), varDecl("x"), null, null))
                                )
                            )
                        )
                    )
                }
            ),
            // Basic SELECT LIST with single item
            TestCase(
                "SELECT f.bar AS b FROM foo AS f",
                PartiqlAlgebra.build {
                    query(
                        mapValues(
                            struct(
                                exprPair(lit(ionSymbol("b")), path(id("f"), pathExpr(lit(ionString("bar")), caseInsensitive())))
                            ),
                            bindingsTerm(scan(id("foo"), varDecl("f"), null, null))
                        )
                    )
                }
            ),
            // Basic SELECT LIST with multiple items
            TestCase(
                "SELECT f.bar AS b, f.bat AS t, f.baz AS z FROM foo AS f",
                PartiqlAlgebra.build {
                    query(
                        mapValues(
                            struct(
                                exprPair(lit(ionSymbol("b")), path(id("f"), pathExpr(lit(ionString("bar")), caseInsensitive()))),
                                exprPair(lit(ionSymbol("t")), path(id("f"), pathExpr(lit(ionString("bat")), caseInsensitive()))),
                                exprPair(lit(ionSymbol("z")), path(id("f"), pathExpr(lit(ionString("baz")), caseInsensitive())))
                            ),
                            bindingsTerm(scan(id("foo"), varDecl("f"), null, null))
                        )
                    )
                }
            )
        )
    }
}
