package org.partiql.lang.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlAlgebraUnindexed
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase

private fun PartiqlAlgebraUnindexed.Builder.id(name: String) =
    this.id(name, caseInsensitive(), unqualified())

class ToUnindexedAlgebraVisitorTransformTests {
    
    private fun parseToAlgebra(sql: String): PartiqlAlgebraUnindexed.Statement {
        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)

        return parser.parseAstStatement(sql)
            .toAstVarDecl()
            .toUnindexedAlgebra()
    }

    data class TestCase(val sql: String, val expectedAlgebra: PartiqlAlgebraUnindexed.Statement)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForAstToUnindexedAlgebraTests::class)
    fun runTestCase(tc: TestCase) {
        val algebra = assertDoesNotThrow("Parsing TestCase.sql should not throw") {
            parseToAlgebra(tc.sql)
        }
        Assertions.assertEquals(tc.expectedAlgebra, algebra)
    }

    class ArgumentsForAstToUnindexedAlgebraTests : ArgumentsProviderBase() {
        // Note that: test cases for SELECT * are not needed since we already have an AST transform which converts
        // it to SELECT f.*, b.*, etc.

        override fun getParameters() = listOf(
            // Basic SELECT VALUE with all three bindings specified
            TestCase(
                "SELECT VALUE foo FROM bar AS b AT c BY d",
                PartiqlAlgebraUnindexed.build {
                    query(
                        mapValues(
                            id("foo"),
                            bindingsTerm(scan(id("bar"), varDecl("b", 0), varDecl("c", 1), varDecl("d", 2)))
                        )
                    )
                }
            ),
            // Basic SELECT VALUE with PATH expression and WHERE clause
            TestCase(
                "SELECT VALUE x.y FROM z AS x WHERE x.y = 42",
                PartiqlAlgebraUnindexed.build {
                    query(
                        mapValues(
                            path(id("x"), pathExpr(lit(ionString("y")), caseInsensitive())),
                            bindingsTerm(
                                filter(
                                    eq(
                                        path(id("x"), pathExpr(lit(ionString("y")), caseInsensitive())),
                                        lit(ionInt(42))
                                    ),
                                    bindingsTerm(scan(id("z"), varDecl("x", 0), null, null))
                                )
                            )
                        )
                    )
                }
            ),
            // Basic SELECT LIST with single item
            TestCase(
                "SELECT f.bar AS b FROM foo AS f",
                PartiqlAlgebraUnindexed.build {
                    query(
                        mapValues(
                            struct(
                                exprPair(lit(ionSymbol("b")), path(id("f"), pathExpr(lit(ionString("bar")), caseInsensitive())))
                            ),
                            bindingsTerm(scan(id("foo"), varDecl("f", 0), null, null))
                        )
                    )
                }
            ),
            // Basic SELECT LIST with multiple items
            TestCase(
                "SELECT f.bar AS b, f.bat AS t, f.baz AS z FROM foo AS f",
                PartiqlAlgebraUnindexed.build {
                    query(
                        mapValues(
                            struct(
                                exprPair(lit(ionSymbol("b")), path(id("f"), pathExpr(lit(ionString("bar")), caseInsensitive()))),
                                exprPair(lit(ionSymbol("t")), path(id("f"), pathExpr(lit(ionString("bat")), caseInsensitive()))),
                                exprPair(lit(ionSymbol("z")), path(id("f"), pathExpr(lit(ionString("baz")), caseInsensitive())))
                            ),
                            bindingsTerm(scan(id("foo"), varDecl("f", 0), null, null))
                        )
                    )
                }
            ),
            TestCase(
                "SELECT a AS aa, b AS bb, c AS cc, d AS dd, e AS ee, f AS ff FROM table_a AS a AT b BY c, table_b AS d AT e BY f",
                PartiqlAlgebraUnindexed.build {
                    query(
                        mapValues(
                            struct(
                                exprPair(lit(ionSymbol("aa")), id("a")),
                                exprPair(lit(ionSymbol("bb")), id("b")),
                                exprPair(lit(ionSymbol("cc")), id("c")),
                                exprPair(lit(ionSymbol("dd")), id("d")),
                                exprPair(lit(ionSymbol("ee")), id("e")),
                                exprPair(lit(ionSymbol("ff")), id("f"))
                            ),
                            bindingsTerm(
                                crossJoin(
                                    bindingsTerm(
                                        scan(id("table_a"), varDecl("a", 0), varDecl("b", 1), varDecl("c", 2))
                                    ),
                                    bindingsTerm(
                                        scan(id("table_b"), varDecl("d", 3), varDecl("e", 4), varDecl("f", 5))
                                    )
                                )
                            )
                        )
                    )
                }
            )
        )
    }

}