package org.partiql.lang.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlAlgebra
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.BindingName
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase


class AstToAlgebraTests {
    data class TestCase(
        val sql: String,
        val expectation: Expectation
    )

    sealed class Expectation {
        data class Success(val expectedAlgebra: PartiqlAlgebra.Statement) : Expectation()
        data class Error(val errorCode: ErrorCode) : Expectation()
    }

    private val tableExists: (BindingName) -> Boolean = { bindingName ->
        when {
            bindingName.isEquivalentTo("f") -> true
            bindingName.isEquivalentTo("foo") -> true
            bindingName.isEquivalentTo("UPPERCASE_FOO") -> true
            else -> false
        }
    }

    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForAstToAlgebraTests::class)
    fun runTestCase(tc: TestCase) {
        val unindexedAlgebra = assertDoesNotThrow {
            parser.parseAstStatement(tc.sql).toAstVarDecl().toUnindexedAlgebra()
        }

        when(tc.expectation) {
            is Expectation.Success -> {
                assertEquals(tc.expectation.expectedAlgebra, unindexedAlgebra.toAlgebra(tableExists))
            }
            is Expectation.Error -> {
                val ex = assertThrows<SemanticException> {
                    unindexedAlgebra.toAlgebra(tableExists)
                }

                assertEquals(tc.expectation.errorCode, ex.errorCode)
            }
        }
    }

    class ArgumentsForAstToAlgebraTests : ArgumentsProviderBase() {
        // Note that: test cases for SELECT * are not needed since we already have an AST transform which converts
        // it to SELECT f.*, b.*, etc.

        // Cases:
        // SELECT VALUE with as, at, by
        // SELECT VALUE with as, at
        // SELECT VALUE with as, by
        // Same as above but with CROSS JOIN.
        // Same as above but with shadowed variables.

        override fun getParameters() = listOf(
            // Case sensitive global lookups...
            TestCase(
                //PartiqlAlgebraUnindexed.build { query(id("foo", caseInsensitive(), unqualified())) },
                "foo",
                Expectation.Success(PartiqlAlgebra.build { query(global("foo", caseInsensitive())) })
            ),
            TestCase(
                //PartiqlAlgebraUnindexed.build { query(id("FOO", caseInsensitive(), unqualified())) },
                "FOO",
                Expectation.Success(PartiqlAlgebra.build { query(global("FOO", caseInsensitive())) })
            ),
            TestCase(
                //PartiqlAlgebraUnindexed.build { query(id("fOo", caseInsensitive(), unqualified())) },
                "fOo",
                Expectation.Success(PartiqlAlgebra.build { query(global("fOo", caseInsensitive())) })
            ),
            TestCase(
                //PartiqlAlgebraUnindexed.build { query(id("foo", caseSensitive(), unqualified())) },
                """ "foo" """,
                Expectation.Success(PartiqlAlgebra.build { query(global("foo", caseSensitive())) })
            ),
            TestCase(
                //PartiqlAlgebraUnindexed.build { query(id("FOO", caseSensitive(), unqualified())) },
                """ "FOO" """,
                Expectation.Error(ErrorCode.SEMANTIC_UNBOUND_BINDING)
            ),
            TestCase(
                //PartiqlAlgebraUnindexed.build { query(id("fOo", caseSensitive(), unqualified())) },
                """ "fOo" """,
                Expectation.Error(ErrorCode.SEMANTIC_UNBOUND_BINDING)
            ),

            createScanTestCase("a", 0),
            createScanTestCase("b", 1),
            createScanTestCase("c", 2),

            TestCase(
                // f on RHS of, should resolve to the global variable
                "SELECT VALUE f FROM foo AS f, f AS ff",
                Expectation.Success(
                    PartiqlAlgebra.build {
                        query(
                            mapValues(
                                id("f", 0),
                                bindingsTerm(
                                    crossJoin(
                                        bindingsTerm(scan(global("foo", caseInsensitive()), varDecl("f", 0), null, null)),
                                        bindingsTerm(scan(global("f", caseInsensitive()), varDecl("ff", 1), null, null))
                                        //                ^ global variable f
                                    )
                                )
                            )
                        )
                    }
                )
            ),
            TestCase(
                // @f on RHS of, should resolve to the local variable
                "SELECT VALUE f FROM foo AS f, @f AS ff",
                Expectation.Success(
                    PartiqlAlgebra.build {
                        query(
                            mapValues(
                                id("f", 0),
                                bindingsTerm(
                                    crossJoin(
                                        bindingsTerm(scan(global("foo", caseInsensitive()), varDecl("f", 0), null, null)),
                                        bindingsTerm(scan(id("f", 0), varDecl("ff", 1), null, null))
                                        //                ^ local variable f
                                    )
                                )
                            )
                        )
                    }
                )
            ),
            TestCase(
                // cross join variable shadowing @f shadowing
                "SELECT VALUE f FROM foo AS f, @f AS f, @f AS f",
                // Variable indexes:        0        1        2
                Expectation.Success(
                    PartiqlAlgebra.build {
                        query(
                            mapValues(
                                id("f", 2),
                                bindingsTerm(
                                    crossJoin(
                                        bindingsTerm(
                                            crossJoin(
                                                bindingsTerm(scan(global("foo", caseInsensitive()), varDecl("f", 0), null, null)),
                                                bindingsTerm(scan(id("f", 0), varDecl("f", 1), null, null)))),
                                        bindingsTerm(scan(id("f", 1), varDecl("f", 2), null, null)))))
                        )
                    }
                )
            ),
            // Demonstrate that globals-first variable lookup only happens in the FROM clause.
            TestCase(
                "SELECT VALUE f FROM f AS f",
                Expectation.Success(
                    PartiqlAlgebra.build {
                        query(
                            mapValues(
                                id("f", 0),
                                bindingsTerm(scan(global("f", caseInsensitive()), varDecl("f", 0), null, null))
                            )
                        )
                    }
                )
            ),
            // Demonstrate that globals-first variable lookup & shadowing happens correctly for nested queries
            TestCase(
                "SELECT VALUE f FROM (SELECT VALUE f FROM f AS f) AS f",
                // Indexes:   1                    0      g    0     1
                Expectation.Success(
                    PartiqlAlgebra.build {
                        query(
                            mapValues(
                                id("f", 1),
                                bindingsTerm(
                                    scan(
                                        mapValues(
                                            id("f", 0),
                                            bindingsTerm(scan(global("f", caseInsensitive()), varDecl("f", 0), null, null))
                                        ),
                                        varDecl("f", 1),
                                        null,
                                        null))
                                )
                            )
                    }
                )
            )
        )

        private fun createScanTestCase(varName: String, expectedIndex: Int) =
            TestCase(
                "SELECT VALUE $varName FROM foo AS a AT b BY c",
                Expectation.Success(
                    PartiqlAlgebra.build {
                        query(
                            mapValues(
                                id(varName, expectedIndex.toLong()),
                                bindingsTerm(
                                    scan(
                                        global("foo", caseInsensitive()),
                                        varDecl("a", 0),
                                        varDecl("b", 1),
                                        varDecl("c", 2)
                                    )
                                )
                            )
                        )
                    }
                )
            )
    }
}
