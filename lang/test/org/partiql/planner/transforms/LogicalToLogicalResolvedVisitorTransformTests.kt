package org.partiql.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.eval.sourceLocationMeta
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.toIntExact
import org.partiql.planner.createFakeGlobalBindings
import org.partiql.planner.problem

class LogicalToLogicalResolvedVisitorTransformTests {
    data class TestCase(
        val sql: String,
        val expectation: Expectation,
        val allowUndefinedVariables: Boolean = false
    )

    data class ResolvedId(
        val line: Int,
        val charOffset: Int,
        val expr: PartiqlLogicalResolved.Expr
    ) {
        constructor(
            line: Int,
            charOffset: Int,
            build: PartiqlLogicalResolved.Builder.() -> PartiqlLogicalResolved.Expr
        ) :this(line, charOffset, PartiqlLogicalResolved.BUILDER().build())

        override fun toString(): String {
            return "($line, $charOffset): $expr"
        }
    }
    sealed class Expectation {
        data class Success(val expectedIds: List<ResolvedId>) : Expectation() {
            constructor(vararg expectedIds: ResolvedId) : this(expectedIds.toList())
        }
        data class Problems(val problems: List<Problem>) : Expectation() {
            constructor(vararg problems: Problem) : this(problems.toList())
        }
    }

    /** Mock table resolver. That can resolve f, foo, or UPPERCASE_FOO, while respecting case-sensitivity. */
    private val globalBindings = createFakeGlobalBindings(
        *listOf(
            "shadow",
            "foo",
            "bar",
            "bat",
            "UPPERCASE_FOO",
            "case_AMBIGUOUS_foo",
            "case_ambiguous_FOO"
        ).map {
            it to "fake_uid_for_$it"
        }.toTypedArray()
    )

    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    private fun runTestCase(tc: TestCase) {
        val algebra: PartiqlLogical.Statement = assertDoesNotThrow {
            parser.parseAstStatement(tc.sql).toLogical()
        }

        val problemHandler = ProblemCollector()

        when (tc.expectation) {
            is Expectation.Success -> {
                val resolved = algebra.toResolved(problemHandler, globalBindings, tc.allowUndefinedVariables)

                // extract all of the dynamic, global and local ids from the resolved logical plan.
                val actualResolvedIds =
                    object : PartiqlLogicalResolved.VisitorFold<List<PartiqlLogicalResolved.Expr>>() {
                        override fun visitExpr(
                            node: PartiqlLogicalResolved.Expr,
                            accumulator: List<PartiqlLogicalResolved.Expr>
                        ): List<PartiqlLogicalResolved.Expr> =
                            when(node) {
                                is PartiqlLogicalResolved.Expr.Id,
                                is PartiqlLogicalResolved.Expr.GlobalId,
                                is PartiqlLogicalResolved.Expr.LocalId -> accumulator + node
                                else -> accumulator
                            }

                        // Don't include children of dynamic id
                        override fun walkExprId(
                            node: PartiqlLogicalResolved.Expr.Id,
                            accumulator: List<PartiqlLogicalResolved.Expr>
                        ): List<PartiqlLogicalResolved.Expr> {
                            return accumulator
                        }
                    }.walkStatement(resolved, emptyList())

                assertEquals(tc.expectation.expectedIds.size, actualResolvedIds.size,
                    "Number of expected resovled variables must match actual"
                )

                val remainingActualResolvedIds = actualResolvedIds.map {
                    val location = it.metas.sourceLocationMeta ?: error("$it missing source location meta")
                    ResolvedId(location.lineNum.toIntExact(), location.charOffset.toIntExact()) { it }
                }.filter { expectedId: ResolvedId ->
                    tc.expectation.expectedIds.none { actualId -> actualId == expectedId }
                }

                if(remainingActualResolvedIds.isNotEmpty()) {
                    System.err.println("Unexpected ids:")
                    remainingActualResolvedIds.forEach {
                        System.err.println(it)
                    }
                    System.err.println("Expected ids:")
                    tc.expectation.expectedIds.forEach {
                        System.err.println(it)
                    }

                    fail("Unmatched resolved ids were found. See stderr for details.")
                }
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
                expectation = Expectation.Success(ResolvedId(1, 1) { globalId("FOO", "fake_uid_for_foo") })
            ),
            TestCase(
                // all lower case
                "foo",
                Expectation.Success(ResolvedId(1, 1) { globalId("foo", "fake_uid_for_foo") })
            ),
            TestCase(
                // mixed case
                "fOo",
                Expectation.Success(ResolvedId(1, 1)  { globalId("fOo", "fake_uid_for_foo") })
            ),
            TestCase(
                // undefined
                """ foobar """,
                Expectation.Problems(
                    problem(
                        1,
                        2,
                        PlanningProblemDetails.UndefinedVariable("foobar", caseSensitive = false)
                    )
                )
            ),

            // Ambiguous case-insensitive lookup
            TestCase(
                // ambiguous
                """case_ambiguous_foo """,
                // In this case, we resolve to the first matching binding.  This is consistent with Postres 9.6.
                Expectation.Success(ResolvedId(1, 1)  {
                    globalId(
                        "case_ambiguous_foo",
                        "fake_uid_for_case_AMBIGUOUS_foo"
                    )
                })
            ),

            // Case-insensitive resolution of global variables with all uppercase letters...
            TestCase(
                // all uppercase
                "UPPERCASE_FOO",
                Expectation.Success(ResolvedId(1, 1)  {
                    globalId(
                        "UPPERCASE_FOO",
                        "fake_uid_for_UPPERCASE_FOO"
                    )
                })
            ),
            TestCase(
                // all lower case
                "uppercase_foo",
                Expectation.Success(ResolvedId(1, 1)  {
                    globalId(
                        "uppercase_foo",
                        "fake_uid_for_UPPERCASE_FOO"
                    )
                })
            ),
            TestCase(
                // mixed case
                "UpPeRcAsE_fOo",
                Expectation.Success(ResolvedId(1, 1)  {
                    globalId(
                        "UpPeRcAsE_fOo",
                        "fake_uid_for_UPPERCASE_FOO"
                    )
                })
            ),

            // undefined variables allowed
            TestCase(
                // undefined allowed (case-insensitive)
                """some_undefined """,
                Expectation.Success(ResolvedId(1, 1)  {
                    id("some_undefined", caseInsensitive(), localsThenGlobals())
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
                "\"FOO\"",
                Expectation.Problems(
                    problem(
                        1,
                        1,
                        PlanningProblemDetails.UndefinedVariable("FOO", caseSensitive = true)
                    )
                )
            ),
            TestCase(
                // all lowercase
                "\"foo\"",
                Expectation.Success(ResolvedId(1, 1)  { globalId("foo", "fake_uid_for_foo") })
            ),
            TestCase(
                // mixed
                "\"foO\"",
                Expectation.Problems(
                    problem(
                        1,
                        1,
                        PlanningProblemDetails.UndefinedVariable("foO", caseSensitive = true)
                    )
                )
            ),

            // Case-sensitive resolution of global variables with all uppercase letters
            TestCase(
                // all uppercase
                "\"UPPERCASE_FOO\"",
                Expectation.Success(ResolvedId(1, 1)  {
                    globalId(
                        "UPPERCASE_FOO",
                        "fake_uid_for_UPPERCASE_FOO"
                    )
                })
            ),
            TestCase(
                // all lowercase
                "\"uppercase_foo\"",
                Expectation.Problems(
                    problem(1, 1, PlanningProblemDetails.UndefinedVariable("uppercase_foo", caseSensitive = true))
                )
            ),
            TestCase(
                // mixed
                "\"UpPeRcAsE_fOo\"",
                Expectation.Problems(
                    problem(1, 1, PlanningProblemDetails.UndefinedVariable("UpPeRcAsE_fOo", caseSensitive = true))
                )
            ),
            TestCase(
                // not ambiguous when case-sensitive
                "\"case_AMBIGUOUS_foo\"",
                Expectation.Success(ResolvedId(1, 1)  {
                    globalId("case_AMBIGUOUS_foo", "fake_uid_for_case_AMBIGUOUS_foo")
                })
            ),
            TestCase(
                // not ambiguous when case-sensitive
                "\"case_ambiguous_FOO\"",
                Expectation.Success(ResolvedId(1, 1)  {
                    globalId("case_ambiguous_FOO", "fake_uid_for_case_ambiguous_FOO")
                })
            ),
            TestCase(
                // undefined
                """ FOOBAR """,
                Expectation.Problems(
                    problem(
                        1,
                        2,
                        PlanningProblemDetails.UndefinedVariable("FOOBAR", caseSensitive = false)
                    )
                )
            ),

            TestCase(
                // undefined allowed (case-sensitive)
                "\"some_undefined\"",
                Expectation.Success(ResolvedId(1, 1)  {
                    id("some_undefined", caseSensitive(), localsThenGlobals())
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
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("FOO", 0) },
                    ResolvedId(1, 34)  { localId("FOO", 0) }
                )
            ),
            TestCase(
                // all lowercase
                "SELECT foo.* FROM 1 AS foo WHERE foo",
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("foo", 0) },
                    ResolvedId(1, 34)  { localId("foo", 0) }
                )
            ),
            TestCase(
                // mixed case
                "SELECT FoO.* FROM 1 AS foo WHERE fOo",
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("FoO", 0) },
                    ResolvedId(1, 34)  { localId("fOo", 0) }
                )
            ),
            TestCase(
                // foobar is undefined (select list)
                "SELECT foobar.* FROM [] AS foo",
                Expectation.Problems(
                    problem(1, 8, PlanningProblemDetails.UndefinedVariable("foobar", caseSensitive = false))
                )
            ),
            TestCase(
                //barbat is undefined (where clause)
                "SELECT foo.* FROM [] AS foo WHERE barbat",
                Expectation.Problems(
                    problem(1, 35, PlanningProblemDetails.UndefinedVariable("barbat", caseSensitive = false))
                )
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
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("foo", 0) },
                    ResolvedId(1, 36)  { localId("foo", 0) },
                ),
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
                Expectation.Problems(
                    problem(1, 8, PlanningProblemDetails.UndefinedVariable("foobar", caseSensitive = true))
                )
            ),
            TestCase(
                // "barbat" is undefined (where clause)
                "SELECT \"foo\".* FROM [] AS foo WHERE \"barbat\"",
                Expectation.Problems(
                    problem(1, 37, PlanningProblemDetails.UndefinedVariable("barbat", caseSensitive = true))
                )
            )
        )
    }

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
                    ResolvedId(1, 8)  { localId(varName, expectedIndex.toLong()) },
                    ResolvedId(1, 17)  { globalId("foo", "fake_uid_for_foo") })
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
                    ResolvedId(1, 8)  { localId("b", 0) },
                    ResolvedId(1, 17)  { globalId("bar", "fake_uid_for_bar") },
                    ResolvedId(1, 32)  { localId("b", 0) },
                )
            ),

            // Demonstrate that globals-first variable lookup only happens in the FROM clause.
            TestCase(
                "SELECT shadow.* FROM shadow AS shadow", // `shadow` defined here shadows the global `shadow`
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("shadow", 0) },
                    ResolvedId(1, 22)  {globalId("shadow", "fake_uid_for_shadow") }
                )
            ),

            // JOIN with shadowing
            TestCase(
                // first `AS s` shadowed by second `AS s`.
                "SELECT s.* FROM 1 AS s, @s AS s",
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("s", 1) },
                    ResolvedId(1, 26) { localId("s", 0) }
                )
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(DynamicIdSearchCases::class)
    fun `dynamic_id search order cases`(tc: TestCase) = runTestCase(tc)
    class DynamicIdSearchCases : ArgumentsProviderBase() {
        // The important thing being asserted here is the contents of the dynamicId.search, which
        // defines the places we'll look for variables that are unresolved at compile time.
        override fun getParameters() = listOf(
            // Not in an SFW query (empty search path)
            TestCase(
                "undefined1 + undefined2",
                Expectation.Success(
                    ResolvedId(1, 1)  { id("undefined1", caseInsensitive(), localsThenGlobals()) },
                    ResolvedId(1, 14)  { id("undefined2", caseInsensitive(), localsThenGlobals()) }
                ),
                allowUndefinedVariables = true
            ),

            // In select list and where clause
            TestCase(
                "SELECT undefined1 AS u FROM 1 AS f WHERE undefined2", // 1 from source
                Expectation.Success(
                    ResolvedId(1, 8)  { id("undefined1", caseInsensitive(), localsThenGlobals(), localId("f", 0)) },
                    ResolvedId(1, 42)  { id("undefined2", caseInsensitive(), localsThenGlobals(), localId("f", 0)) }
                ),
                allowUndefinedVariables = true
            ),
            TestCase(
                sql = "SELECT undefined1 AS u FROM 1 AS a, 2 AS b WHERE undefined2", // 2 from sources
                Expectation.Success(
                    ResolvedId(1, 8) { id("undefined1", caseInsensitive(), localsThenGlobals(), localId("b", 1), localId("a", 0)) },
                    ResolvedId(1, 50) { id("undefined2", caseInsensitive(), localsThenGlobals(), localId("b", 1), localId("a", 0)) }
                ),
                allowUndefinedVariables = true
            ),
            TestCase(
                sql = "SELECT undefined1 AS u FROM 1 AS f, 1 AS b, 1 AS t WHERE undefined2", // 3 from sources
                Expectation.Success(
                    ResolvedId(1, 8) {
                        id("undefined1", caseInsensitive(), localsThenGlobals(), localId("t", 2), localId("b", 1), localId("f", 0))
                    },
                    ResolvedId(1, 58) {
                        id("undefined2", caseInsensitive(), localsThenGlobals(), localId("t", 2), localId("b", 1), localId("f", 0))
                    }
                ),
                allowUndefinedVariables = true
            )
        )
    }


    @ParameterizedTest
    @ArgumentsSource(SubqueryCases::class)
    fun `sub-queries`(tc: TestCase) = runTestCase(tc)
    class SubqueryCases : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TestCase(
                // inner query does not reference variables outer query
                "SELECT b.* FROM (SELECT a.* FROM 1 AS a) AS b",
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("b", 1) },
                    ResolvedId(1, 25)  { localId("a", 0) },
                )
            ),
            TestCase(
                // inner query references variable from outer query.
                "SELECT a.*, b.* FROM 1 AS a, (SELECT a.*, b.* FROM 1 AS x) AS b",
                Expectation.Success(
                    // The variables reference in the outer query
                    ResolvedId(1, 8)  { localId("a", 0) },
                    ResolvedId(1, 13)  { localId("b", 2) },
                    // The variables reference in the inner query
                    ResolvedId(1, 38)  { localId("a", 0) },
                    // Note that `b` from the outer query is not accessible inside the query so we fall back on dynamic lookup
                    ResolvedId(1, 43)  { id("b", caseInsensitive(), localsThenGlobals(), localId("x", 1), localId("a", 0)) }
                ),
                allowUndefinedVariables = true
            ),

            // In FROM source
            TestCase(
                "SELECT f.*, u.* FROM 1 AS f, undefined AS u",
                Expectation.Success(
                    ResolvedId(1, 8)  { localId("f", 0) },
                    ResolvedId(1, 13)  { localId("u", 1) },
                    ResolvedId(1, 30)  { id("undefined", caseInsensitive(), globalsThenLocals(), localId("f", 0)) }
                ),
                allowUndefinedVariables = true
            ),
        )
    }
}

