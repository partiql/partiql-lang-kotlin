package org.partiql.lang.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.id
import org.partiql.lang.domains.pathExpr
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.unimplementedProblem
import org.partiql.lang.syntax.PartiQLParser
import org.partiql.lang.util.ArgumentsProviderBase

/**
 * Test cases in this class might seem a little light--that's because [AstToLogicalVisitorTransform] is getting
 * heavily exercised during many other integration tests.  These should be considered "smoke tests".
 */
class AstToLogicalVisitorTransformTests {
    private val ion = IonSystemBuilder.standard().build()
    private val parser = PartiQLParser(ion)

    private fun parseAndTransform(sql: String, problemHandler: ProblemHandler): PartiqlLogical.Statement {
        val parseAstStatement = parser.parseAstStatement(sql)
        return parseAstStatement.toLogicalPlan(problemHandler).stmt
    }

    data class TestCase(val sql: String, val expectedAlgebra: PartiqlLogical.Statement)

    private fun runTestCase(tc: TestCase) {
        val problemHandler = ProblemCollector()
        val algebra = assertDoesNotThrow("Parsing TestCase.sql should not throw") {
            parseAndTransform(tc.sql, problemHandler)
        }
        assertEquals(
            0,
            problemHandler.problems.filter { it.details.severity == ProblemSeverity.WARNING }.size,
            "No problems were expected"
        )

        // println(SexpAstPrettyPrinter.format(algebra.toIonElement().asAnyElement().toIonValue(ion)))
        assertEquals(tc.expectedAlgebra, algebra)
    }

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToLogicalSfwTests::class)
    fun `to logical (SFW)`(tc: TestCase) = runTestCase(tc)

    class ArgumentsForToLogicalSfwTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TestCase(
                // Note:
                // `SELECT * FROM bar AS b` is rewritten to `SELECT b.* FROM bar as b` by [SelectStarVisitorTransform].
                // Therefore, there is no need to support `SELECT *` in `AstToLogicalVisitorTransform`.
                "SELECT b.* FROM bar AS b",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structFields(id("b"))),
                            scan(id("bar"), varDecl("b"))
                        )
                    )
                }
            ),
            TestCase(
                // Note: This is supported by the AST -> logical -> physical transformation but should be rejected
                // by the planner since it is a full table scan, which we won't support initially.
                "SELECT b.* FROM bar AS b WHERE TRUE = TRUE",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structFields(id("b"))),
                            filter(
                                eq(lit(ionBool(true)), lit(ionBool(true))),
                                scan(id("bar"), varDecl("b"))
                            )
                        )
                    )
                }
            ),
            TestCase(
                "SELECT b.* FROM bar AS b WHERE b.primaryKey = 42",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structFields(id("b"))),
                            filter(
                                eq(path(id("b"), pathExpr(lit(ionString("primaryKey")))), lit(ionInt(42))),
                                scan(id("bar"), varDecl("b"))
                            )
                        )
                    )
                }
            ),
            TestCase(
                "SELECT DISTINCT b.* FROM bar AS b",
                PartiqlLogical.build {
                    query(
                        call(
                            "filter_distinct",
                            bindingsToValues(
                                struct(structFields(id("b"))),
                                scan(id("bar"), varDecl("b"))
                            )
                        )
                    )
                }
            ),
            TestCase(
                "SELECT v.*, n.* FROM UNPIVOT bar AS v AT n",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(
                                structFields(id("v")),
                                structFields(id("n"))
                            ),
                            unpivot(id("bar"), varDecl("v"), varDecl("n"))
                        )
                    )
                }
            ),
            TestCase(
                "SELECT b.* FROM bar AS b ORDER BY y",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structFields(id("b"))),
                            sort(
                                scan(id("bar"), varDecl("b")),
                                sortSpec(id("y"), asc(), nullsLast())
                            )
                        )
                    )
                }
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToLogicalDmlTests::class)
    fun `to logical (DML)`(tc: TestCase) = runTestCase(tc)
    class ArgumentsForToLogicalDmlTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TestCase(
                "INSERT INTO foo << 1 >>",
                PartiqlLogical.build {
                    dml(
                        identifier("foo", caseInsensitive()),
                        dmlInsert(),
                        bag(lit(ionInt(1)))
                    )
                }
            ),

            TestCase(
                "INSERT INTO foo SELECT x.* FROM 1 AS x",
                PartiqlLogical.build {
                    dml(
                        identifier("foo", caseInsensitive()),
                        dmlInsert(),
                        bindingsToValues(
                            struct(structFields(id("x", caseInsensitive(), unqualified()))),
                            scan(lit(ionInt(1)), varDecl("x"))
                        )
                    )
                }
            ),
            TestCase(
                "INSERT INTO foo SELECT x.* FROM 1 AS x ON CONFLICT DO REPLACE EXCLUDED",
                PartiqlLogical.build {
                    dml(
                        identifier("foo", caseInsensitive()),
                        dmlReplace(),
                        bindingsToValues(
                            struct(structFields(id("x", caseInsensitive(), unqualified()))),
                            scan(lit(ionInt(1)), varDecl("x"))
                        )
                    )
                }
            ),
            TestCase(
                "INSERT INTO foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            identifier("f", caseInsensitive()),
                            dmlReplace(),
                            bag(
                                struct(
                                    structField(lit(ionString("id")), lit(ionInt(1))),
                                    structField(lit(ionString("name")), lit(ionString("bob")))
                                )
                            )
                        )
                    }
                }
            ),
            TestCase(
                "INSERT INTO foo SELECT x.* FROM 1 AS x ON CONFLICT DO UPDATE EXCLUDED",
                PartiqlLogical.build {
                    dml(
                        identifier("foo", caseInsensitive()),
                        dmlUpdate(),
                        bindingsToValues(
                            struct(structFields(id("x", caseInsensitive(), unqualified()))),
                            scan(lit(ionInt(1)), varDecl("x"))
                        )
                    )
                }
            ),
            TestCase(
                "INSERT INTO foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            identifier("f", caseInsensitive()),
                            dmlUpdate(),
                            bag(
                                struct(
                                    structField(lit(ionString("id")), lit(ionInt(1))),
                                    structField(lit(ionString("name")), lit(ionString("bob")))
                                )
                            )
                        )
                    }
                }
            ),
            TestCase(
                "REPLACE INTO foo AS f <<{'id': 1, 'name':'bob'}>>",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            identifier("f", caseInsensitive()),
                            dmlReplace(),
                            bag(
                                struct(
                                    structField(lit(ionString("id")), lit(ionInt(1))),
                                    structField(lit(ionString("name")), lit(ionString("bob")))
                                )
                            )
                        )
                    }
                }
            ),
            TestCase(
                "UPSERT INTO foo AS f SELECT x.* FROM 1 AS x",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            identifier("f", caseInsensitive()),
                            dmlUpdate(),
                            bindingsToValues(
                                struct(structFields(id("x", caseInsensitive(), unqualified()))),
                                scan(lit(ionInt(1)), varDecl("x"))
                            )
                        )
                    }
                }
            ),
            TestCase(
                "DELETE FROM y AS y",
                PartiqlLogical.build {
                    dml(
                        identifier("y", caseInsensitive()),
                        dmlDelete(),
                        bindingsToValues(
                            id("y", caseSensitive(), unqualified()),
                            scan(id("y", caseInsensitive(), unqualified()), varDecl("y"))
                        )
                    )
                }
            ),
            TestCase(
                "DELETE FROM y AS y WHERE 1=1",
                PartiqlLogical.build {
                    dml(
                        identifier("y", caseInsensitive()),
                        dmlDelete(),
                        bindingsToValues(
                            id("y", caseSensitive(), unqualified()),
                            // this logical plan is same as previous but includes this filter
                            filter(
                                eq(lit(ionInt(1)), lit(ionInt(1))),
                                scan(id("y", caseInsensitive(), unqualified()), varDecl("y"))
                            )
                        )
                    )
                }
            ),
            TestCase(
                "PIVOT x.v AT x.a FROM << {'a': 'first', 'v': 'john'}, {'a': 'last', 'v': 'doe'} >> as x",
                PartiqlLogical.build {
                    query(
                        pivot(
                            input = scan(
                                bag(
                                    struct(
                                        listOf(
                                            structField(lit(ionString("a")), lit(ionString("first"))),
                                            structField(lit(ionString("v")), lit(ionString("john"))),
                                        )
                                    ),
                                    struct(
                                        listOf(
                                            structField(lit(ionString("a")), lit(ionString("last"))),
                                            structField(lit(ionString("v")), lit(ionString("doe"))),
                                        )
                                    )
                                ),
                                asDecl = varDecl("x"),
                            ),
                            key = path(id("x"), listOf(pathExpr(lit(ionString("v"))))),
                            value = path(id("x"), listOf(pathExpr(lit(ionString("a"))))),
                        )
                    )
                }
            )
        )
    }

    data class ProblemTestCase(val sql: String, val expectedProblem: Problem)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForProblemTests::class)
    fun `unimplemented feautres are blocked`(tc: ProblemTestCase) {
        val problemHandler = ProblemCollector()
        assertDoesNotThrow("Parsing TestCase.sql should not throw") {
            parseAndTransform(tc.sql, problemHandler)
        }

        assertFalse(problemHandler.hasWarnings, "didn't expect any warnings")
        assertTrue(problemHandler.hasErrors, "at least one error was expected")

        assertEquals(tc.expectedProblem, problemHandler.problems.first())
    }

    /**
     * Below are all statements that cannot be converted into the logical algebra yet by [AstToLogicalVisitorTransform].
     * This is temporary--in the near future, we will accomplish this with a better language restriction feature which
     * blocks all language features except those explicitly allowed.  This will be needed to constrain possible queries
     * to features supported by specific PartiQL-services.
     */
    class ArgumentsForProblemTests : ArgumentsProviderBase() {

        override fun getParameters() = listOf(
            // SELECT queries are not implemented
            ProblemTestCase("SELECT b.* FROM bar AS b GROUP BY a", unimplementedProblem("GROUP BY", 1, 26)),
            ProblemTestCase("SELECT b.* FROM bar AS b HAVING x", unimplementedProblem("HAVING", 1, 33)),

            // DDL is  not implemented
            ProblemTestCase("CREATE TABLE foo", unimplementedProblem("CREATE TABLE", 1, 1)),
            ProblemTestCase("DROP TABLE foo", unimplementedProblem("DROP TABLE", 1, 1)),
            ProblemTestCase("CREATE INDEX ON foo (x)", unimplementedProblem("CREATE INDEX", 1, 1)),
            ProblemTestCase("DROP INDEX bar ON foo", unimplementedProblem("DROP INDEX", 1, 1)),

            // Unimplemented parts of DML
            ProblemTestCase("FROM x AS xx INSERT INTO foo VALUES (1, 2)", unimplementedProblem("UPDATE / INSERT", 1, 14)),
            ProblemTestCase("FROM x AS xx SET k = 5", unimplementedProblem("SET", 1, 14)),
            ProblemTestCase("UPDATE x SET k = 5", unimplementedProblem("SET", 1, 10)),
            ProblemTestCase("UPDATE x REMOVE k", unimplementedProblem("REMOVE", 1, 10)),
            ProblemTestCase("UPDATE x INSERT INTO k << 1 >>", unimplementedProblem("UPDATE / INSERT", 1, 10)),

            // INSERT INTO ... VALUE ... is not supported because it is redundant with INSERT INTO ... << <expr> >>
            ProblemTestCase(
                "INSERT INTO x VALUE 1",
                Problem(SourceLocationMeta(1, 1), PlanningProblemDetails.InsertValueDisallowed)
            ),
            // We need schema to support using INSERT INTO without an explicit list of fields.
            ProblemTestCase(
                "INSERT INTO x VALUES (1, 2, 3)",
                Problem(SourceLocationMeta(1, 1), PlanningProblemDetails.InsertValuesDisallowed)
            )
        )
    }
}
