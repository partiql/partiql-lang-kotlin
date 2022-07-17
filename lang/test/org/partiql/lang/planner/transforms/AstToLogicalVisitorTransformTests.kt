package org.partiql.lang.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.id
import org.partiql.lang.domains.pathExpr
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase

/**
 * Test cases in this class might seem a little light--that's because [AstToLogicalVisitorTransform] is getting
 * heavily exercised during many other integration tests.  These should be considered "smoke tests".
 */
class AstToLogicalVisitorTransformTests {
    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    private fun parseAndTransform(sql: String): PartiqlLogical.Statement {
        val parseAstStatement = parser.parseAstStatement(sql)
        // println(SexpAstPrettyPrinter.format(parseAstStatement.toIonElement().asAnyElement().toIonValue(ion)))
        return parseAstStatement.toLogicalPlan().stmt
    }

    data class TestCase(val sql: String, val expectedAlgebra: PartiqlLogical.Statement)

    private fun runTestCase(tc: TestCase) {
        val algebra = assertDoesNotThrow("Parsing TestCase.sql should not throw") {
            parseAndTransform(tc.sql)
        }
        // println(SexpAstPrettyPrinter.format(algebra.toIonElement().asAnyElement().toIonValue(ion)))
        Assertions.assertEquals(tc.expectedAlgebra, algebra)
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
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToLogicalDmlTests::class)
    fun `to logical (DML)`(tc: TestCase) = runTestCase(tc)
    class ArgumentsForToLogicalDmlTests : ArgumentsProviderBase() {
        private val insertIntoFooBagOf1 = PartiqlLogical.build {
            dml(
                dmlTarget(id("foo", caseInsensitive(), unqualified())),
                dmlInsert(),
                bag(lit(ionInt(1)))
            )
        }
        override fun getParameters() = listOf(
            // these two semantically identical cases result in the same logical plan
            // TestCase("INSERT INTO foo VALUE 1", insertIntoFooBagOf1),
            TestCase("INSERT INTO foo << 1 >>", insertIntoFooBagOf1),

            TestCase(
                "INSERT INTO foo SELECT x.* FROM 1 AS x",
                PartiqlLogical.build {
                    dml(
                        dmlTarget(id("foo", caseInsensitive(), unqualified())),
                        dmlInsert(),
                        bindingsToValues(
                            struct(structFields(id("x", caseInsensitive(), unqualified()))),
                            scan(lit(ionInt(1)), varDecl("x"))
                        )
                    )
                }
            ),
            TestCase(
                "DELETE FROM y AS y",
                PartiqlLogical.build {
                    dml(
                        dmlTarget(id("y", caseInsensitive(), unqualified())),
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
                        dmlTarget(id("y", caseInsensitive(), unqualified())),
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
        )
    }

    data class TodoTestCase(val sql: String)
    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToToDoTests::class)
    fun todo(tc: TodoTestCase) {
        assertThrows<NotImplementedError>("Parsing TestCase.sql should throw NotImplementedError") {
            parseAndTransform(tc.sql)
        }
    }

    /**
     * A list of statements that cannot be converted into the logical algebra yet by [AstToLogicalVisitorTransform].
     * This is temporary--in the near future, we will accomplish this with a better language restriction feature which
     * blocks all language features except those explicitly allowed.  This will be needed to constrain possible queries
     * to features supported by specific PartiQL-services.
     */
    class ArgumentsForToToDoTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // SELECT queries
            TodoTestCase("SELECT b.* FROM UNPIVOT x as y"),
            TodoTestCase("SELECT b.* FROM bar AS b GROUP BY a"),
            TodoTestCase("SELECT b.* FROM bar AS b HAVING x"),
            TodoTestCase("SELECT b.* FROM bar AS b ORDER BY y"),
            TodoTestCase("PIVOT v AT n FROM data AS d"),

            // DDL
            TodoTestCase("CREATE TABLE foo"),
            TodoTestCase("DROP TABLE foo"),
            TodoTestCase("CREATE INDEX ON foo (x)"),
            TodoTestCase("DROP INDEX bar ON foo"),

            // DML
            // DL TODO: include DELETE with non-scan relational operators
            // DL TODO: TodoTestCase("INSERT INTO foo VALUES(1)"),
            TodoTestCase("FROM x AS xx INSERT INTO foo VALUES (1, 2)"),
            TodoTestCase("FROM x AS xx WHERE a = b SET k = 5"),
            TodoTestCase("UPDATE x SET k = 5"),
            TodoTestCase("UPDATE x INSERT INTO k << 1 >>"),
        )
    }
    // DL TODO: include AT, BY aliases
    // DL TODO: scan AstToLogicalVisitorTransform.kt for additional error cases and test them.
}
