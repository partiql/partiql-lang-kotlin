package org.partiql.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.planner.createFakeGlobalBindings
import org.partiql.planner.problem

// DL TODO: add some tests that allow unresolved variables.

class LogicalToLogicalResolvedVisitorTransformTests {
    data class TestCase(
        val sql: String,
        val expectation: Expectation,
        val allowUndefinedVariables: Boolean = false
    )

    sealed class Expectation {
        data class Success(val expectedAlgebra: PartiqlLogicalResolved.Statement) : Expectation()
        data class Problems(val problems: List<Problem>) : Expectation() {
            constructor(vararg problems: Problem) : this(problems.toList())
        }
    }

    /** Mock table resolver. That can resolve f, foo, or UPPERCASE_FOO, while respecting case-sensitivity. */
    private val globalBindings = createFakeGlobalBindings(
        "shadow",
        "foo",
        "bar",
        "UPPERCASE_FOO",
        "case_AMBIGUOUS_foo",
        "case_ambiguous_FOO"
    )

    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    private fun runTestCase(tc: TestCase) {
        val algebra: PartiqlLogical.Statement = assertDoesNotThrow {
            parser.parseAstStatement(tc.sql).toLogical()
        }

        val problemHandler = ProblemCollector()

        when(tc.expectation) {
            is Expectation.Success -> {
                val resolved = algebra.toResolved(problemHandler, globalBindings, tc.allowUndefinedVariables)
                assertEquals(emptyList<Problem>(), problemHandler.problems)
                assertEquals(tc.expectation.expectedAlgebra, resolved)
                assertEquals(emptyList<Problem>(), problemHandler.problems, "Expected success, but there were problems!")
            }
            is Expectation.Problems -> {
                assertDoesNotThrow("Should not throw when variables are undefined") {
                    algebra.toResolved(problemHandler, globalBindings)
                }
                assertEquals(tc.expectation.problems, problemHandler.problems)
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(CaseInsensitiveGlobalsCases::class)
    fun `case-insensitive globals`(tc: TestCase) = runTestCase(tc)
    class CaseInsensitiveGlobalsCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Case-insensitive resolution of global variables...
            TestCase(
                // all uppercase
                sql = "FOO",
                expectation = Expectation.Success(PartiqlLogicalResolved.build { query(globalId("FOO", "fake_uid_for_foo")) })
            ),
            TestCase(
                // all lower case
                "foo",
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("foo", "fake_uid_for_foo")) })
            ),
            TestCase(
                // mixed case
                "fOo",
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("fOo", "fake_uid_for_foo")) })
            ),
            TestCase(
                // undefined
                """ foobar """,
                Expectation.Problems(problem(1, 2, PlanningProblemDetails.UndefinedVariable("foobar", caseSensitive = false)))
            ),

            // Ambiguous case-insensitive lookup
            TestCase(
                // ambiguous
                """ case_ambiguous_foo """,
                // In this case, we resolve to the first matching binding.  This is consistent with Postres 9.6.
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("case_ambiguous_foo", "fake_uid_for_case_AMBIGUOUS_foo")) })
            ),

            // Case-insensitive resolution of global variables with all uppercase letters...
            TestCase(
                // all uppercase
                "UPPERCASE_FOO",
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("UPPERCASE_FOO", "fake_uid_for_UPPERCASE_FOO")) })
            ),
            TestCase(
                // all lower case
                "uppercase_foo",
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("uppercase_foo", "fake_uid_for_UPPERCASE_FOO")) })
            ),
            TestCase(
                // mixed case
                "UpPeRcAsE_fOo",
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("UpPeRcAsE_fOo", "fake_uid_for_UPPERCASE_FOO")) })
            ),

            // undefined variables allowed

            TestCase(
                // undefined allowed (case-insensitive)
                """ some_undefined """,
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(dynamicId("some_undefined", caseInsensitive()))
                }),
                allowUndefinedVariables = true
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(CaseSensitiveGlobalsCases::class)
    fun `case-sensitive globals`(tc: TestCase) = runTestCase(tc)
    class CaseSensitiveGlobalsCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Case-sensitive resolution of global variable with all lowercase letters
            TestCase(
                // all uppercase
                """ "FOO" """,
                Expectation.Problems(problem(1, 2, PlanningProblemDetails.UndefinedVariable("FOO", caseSensitive = true)))
            ),
            TestCase(
                // all lowercase
                """ "foo" """,
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("foo", "fake_uid_for_foo")) })
            ),
            TestCase(
                // mixed
                """ "foO" """,
                Expectation.Problems(problem(1, 2, PlanningProblemDetails.UndefinedVariable("foO", caseSensitive = true)))
            ),

            // Case-sensitive resolution of global variables with all uppercase letters
            TestCase(
                // all uppercase
                """ "UPPERCASE_FOO" """,
                Expectation.Success(PartiqlLogicalResolved.build { query(globalId("UPPERCASE_FOO", "fake_uid_for_UPPERCASE_FOO")) })
            ),
            TestCase(
                // all lowercase
                """ "uppercase_foo" """,
                Expectation.Problems(problem(1, 2, PlanningProblemDetails.UndefinedVariable("uppercase_foo", caseSensitive = true)))
            ),
            TestCase(
                // mixed
                """ "UpPeRcAsE_fOo" """,
                Expectation.Problems(problem(1, 2, PlanningProblemDetails.UndefinedVariable("UpPeRcAsE_fOo", caseSensitive = true)))
            ),
            TestCase(
                // not ambiguous when case-sensitive
                "\"case_AMBIGUOUS_foo\"",
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(globalId("case_AMBIGUOUS_foo", "fake_uid_for_case_AMBIGUOUS_foo"))
                })
            ),
            TestCase(
                // not ambiguous when case-sensitive
                "\"case_ambiguous_FOO\"",
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(globalId("case_ambiguous_FOO", "fake_uid_for_case_ambiguous_FOO"))
                })
            ),
            TestCase(
                // undefined
                """ FOOBAR """,
                Expectation.Problems(problem(1, 2, PlanningProblemDetails.UndefinedVariable("FOOBAR", caseSensitive = false)))
            ),

            TestCase(
                // undefined allowed (case-sensitive)
                """ "some_undefined" """,
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(dynamicId("some_undefined", caseSensitive()))
                }),
                allowUndefinedVariables = true
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(CaseInsensitiveLocalsVariablesCases::class)
    fun `case-insensitive local variables`(tc: TestCase) = runTestCase(tc)
    class CaseInsensitiveLocalsVariablesCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Case-insensitive resolution of local variables with all lowercase letters...
            TestCase(
                // all uppercase
                "SELECT FOO.* FROM 1 AS foo WHERE FOO",
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(
                        bindingsToValues(
                            localId("FOO", 0),
                            filter(
                                localId("FOO", 0),
                                scan(lit(ionInt(1)), varDecl("foo", 0))
                            )
                        )
                    )
                })
            ),
            TestCase(
                // all lowercase
                "SELECT foo.* FROM 1 AS foo WHERE foo",
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(
                        bindingsToValues(
                            localId("foo", 0),
                            filter(
                                localId("foo", 0),
                                scan(lit(ionInt(1)), varDecl("foo", 0))
                            )
                        )
                    )
                }),
            ),
            TestCase(
                // mixed case
                "SELECT FoO.* FROM 1 AS foo WHERE fOo",
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(
                        bindingsToValues(
                            localId("FoO", 0),
                            filter(
                                localId("fOo", 0),
                                scan(lit(ionInt(1)), varDecl("foo", 0))
                            )
                        )
                    )
                })
            ),
            TestCase(
                // foobar is undefined (select list)
                "SELECT foobar.* FROM [] AS foo",
                Expectation.Problems(problem(1, 8, PlanningProblemDetails.UndefinedVariable("foobar", caseSensitive = false)))
            ),
            TestCase(
                //barbat is undefined (where clause)
                "SELECT foo.* FROM [] AS foo WHERE barbat",
                Expectation.Problems(problem(1, 35, PlanningProblemDetails.UndefinedVariable("barbat", caseSensitive = false)))
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(CaseSensitiveLocalVariablesCases::class)
    fun `case-sensitive locals variables`(tc: TestCase) = runTestCase(tc)
    class CaseSensitiveLocalVariablesCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf<TestCase>(
            // Case-insensitive resolution of local variables with all lowercase letters...
            TestCase(
                // all uppercase
                "SELECT \"FOO\".* FROM 1 AS foo WHERE \"FOO\"",
                Expectation.Problems(
                    problem(1, 8, PlanningProblemDetails.UndefinedVariable("FOO", caseSensitive = true)),
                    problem(1, 36, PlanningProblemDetails.UndefinedVariable("FOO", caseSensitive = true))
                )
            ),
            TestCase(
                // all lowercase
                "SELECT \"foo\".* FROM 1 AS foo WHERE \"foo\"",
                Expectation.Success(PartiqlLogicalResolved.build {
                    query(
                        bindingsToValues(
                            localId("foo", 0),
                            filter(
                                localId("foo", 0),
                                scan(lit(ionInt(1)), varDecl("foo", 0))
                            )
                        )
                    )
                }),
            ),
            TestCase(
                // mixed case
                "SELECT \"FoO\".* FROM 1 AS foo WHERE \"fOo\"",
                Expectation.Problems(
                    problem(1, 8, PlanningProblemDetails.UndefinedVariable("FoO", caseSensitive = true)),
                    problem(1, 36, PlanningProblemDetails.UndefinedVariable("fOo", caseSensitive = true))
                )
            ),
            TestCase(
                // "foobar" is undefined (select list)
                "SELECT \"foobar\".* FROM [] AS foo ",
                Expectation.Problems(problem(1, 8, PlanningProblemDetails.UndefinedVariable("foobar", caseSensitive = true)))
            ),
            TestCase(
                // "barbat" is undefined (where clause)
                "SELECT \"foo\".* FROM [] AS foo WHERE \"barbat\"",
                Expectation.Problems(problem(1, 37, PlanningProblemDetails.UndefinedVariable("barbat", caseSensitive = true)))
            )
        )
    }

    // TODO: cases for ambiguity between local and global lookups
    
    @ParameterizedTest
    @ArgumentsSource(DuplicateVariableCases::class)
    fun `duplicate variables`(tc: TestCase) = runTestCase(tc)
    class DuplicateVariableCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Duplicate variables with same case
            TestCase(
                "SELECT {}.* FROM 1 AS a AT a",
                Expectation.Problems(problem(1, 28, PlanningProblemDetails.VariablePreviouslyDefined("a"))),
            ),
            TestCase(
                "SELECT {}.* FROM 1 AS a BY a",
                Expectation.Problems(problem(1, 28, PlanningProblemDetails.VariablePreviouslyDefined("a"))),
            ),
            TestCase(
                "SELECT {}.* FROM 1 AS notdup AT a BY a",
                Expectation.Problems(problem(1, 38, PlanningProblemDetails.VariablePreviouslyDefined("a"))),
            ),
            TestCase(
                "SELECT {}.* FROM 1 AS a AT a BY a",
                Expectation.Problems(
                    problem(1, 28, PlanningProblemDetails.VariablePreviouslyDefined("a")),
                    problem(1, 33, PlanningProblemDetails.VariablePreviouslyDefined("a"))
                ),
            ),
            // Duplicate variables with different cases
            TestCase(
                "SELECT {}.* FROM 1 AS a AT A",
                Expectation.Problems(problem(1, 28, PlanningProblemDetails.VariablePreviouslyDefined("A"))),
            ),
            TestCase(
                "SELECT {}.* FROM 1 AS A BY a",
                Expectation.Problems(problem(1, 28, PlanningProblemDetails.VariablePreviouslyDefined("a"))),
            ),
            TestCase(
                "SELECT {}.* FROM 1 AS notdup AT a BY A",
                Expectation.Problems(problem(1, 38, PlanningProblemDetails.VariablePreviouslyDefined("A"))),
            ),
            TestCase(
                "SELECT {}.* FROM 1 AS foo AT fOo BY foO",
                Expectation.Problems(
                    problem(1, 30, PlanningProblemDetails.VariablePreviouslyDefined("fOo")),
                    problem(1, 37, PlanningProblemDetails.VariablePreviouslyDefined("foO"))
                ),
            )
            // Future test cases:  duplicate variables across joins, i.e. `foo AS a, bar AS a`, etc.
        )
    }

    @ParameterizedTest
    @ArgumentsSource(MiscLocalVariableCases::class)
    fun `misc local variable`(tc: TestCase) = runTestCase(tc)
    class MiscLocalVariableCases : ArgumentsProviderBase() {
        private fun createScanTestCase(varName: String, expectedIndex: Int) =
            TestCase(
                "SELECT $varName.* FROM foo AS a AT b BY c",
                Expectation.Success(
                    PartiqlLogicalResolved.build {
                        query(
                            bindingsToValues(
                                localId(varName, expectedIndex.toLong()),
                                scan(
                                    globalId("foo", "fake_uid_for_foo"),
                                    varDecl("a", 0),
                                    varDecl("b", 1),
                                    varDecl("c", 2)
                                )
                            )
                        )
                    }
                )
            )
        override fun getParameters() = listOf(
            // Demonstrates that FROM source AS aliases work
            createScanTestCase("a", 0),
            // Demonstrates that FROM source AT aliases work
            createScanTestCase("b", 1),
            // Demonstrates that FROM source BY aliases work
            createScanTestCase("c", 2),

            // Covers local variables in select list, global variables in FROM source, local variables in WHERE clause
            TestCase(
                "SELECT b.* FROM bar AS b WHERE b.primaryKey = 42",
                Expectation.Success(
                    PartiqlLogicalResolved.build {
                        query(
                            bindingsToValues(
                                localId("b", 0),
                                filter(
                                    eq(
                                        path(localId("b", 0), pathExpr(lit(ionString("primaryKey")), caseInsensitive())),
                                        lit(ionInt(42))
                                    ),
                                    scan(
                                        expr = globalId(name = "bar", uniqueId = "fake_uid_for_bar"),
                                        asDecl = varDecl("b", 0),
                                        atDecl = null,
                                        byDecl = null
                                    )
                                )
                            )
                        )
                    }
                )
            ),

            // Demonstrate that globals-first variable lookup only happens in the FROM clause.
            TestCase(
                "SELECT shadow.* FROM shadow AS shadow", // `shadow` defined here shadows the global `shadow`
                Expectation.Success(
                    PartiqlLogicalResolved.build {
                        query(
                            bindingsToValues(
                                localId("shadow", 0), // <-- local variable f
                                scan(
                                    expr = globalId(name = "shadow", uniqueId = "fake_uid_for_shadow"), // <-- global variable f.
                                    asDecl = varDecl("shadow", 0),
                                    atDecl = null,
                                    byDecl = null
                                )
                            )
                        )
                    }
                )
            ),
        )
    }
}

