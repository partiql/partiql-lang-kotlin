package org.partiql.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.toIonValue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.id
import org.partiql.lang.domains.pathExpr
import org.partiql.lang.planner.transforms.toLogical
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.SexpAstPrettyPrinter

/**
 * Test cases in this class might seem a little light--that's because [AstToLogicalVisitorTransform] is getting
 * heavily exercised during many other integration tests.  These should be considered "smoke tests".
 */
class AstToLogicalVisitorTransformTests {
    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    private fun parseAndTransform(sql: String): PartiqlLogical.Statement {
        val parseAstStatement = parser.parseAstStatement(sql)
        println(SexpAstPrettyPrinter.format(parseAstStatement.toIonElement().asAnyElement().toIonValue(ion)))
        return parseAstStatement.toLogical()
    }

    data class TestCase(val sql: String, val expectedAlgebra: PartiqlLogical.Statement)

    private fun runTestCase(tc: TestCase) {
        val algebra = assertDoesNotThrow("Parsing TestCase.sql should not throw") {
            parseAndTransform(tc.sql)
        }
        println(SexpAstPrettyPrinter.format(algebra.toIonElement().asAnyElement().toIonValue(ion)))
        Assertions.assertEquals(tc.expectedAlgebra, algebra)
    }

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToLogicalTests::class)
    fun `to logical`(tc: TestCase) = runTestCase(tc)

    class ArgumentsForToLogicalTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TestCase(
                // Note:
                // `SELECT * FROM bar AS b` is rewritten to `SELECT b.* FROM bar as b` by [SelectStarVisitorTransform].
                // Therefore, there is no need to support `SELECT *` in `ToLogicalVisitorTransform`.
                "SELECT b.* FROM bar AS b",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            mergeStruct(structFields(id("b"))),
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
                            mergeStruct(structFields(id("b"))),
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
                            mergeStruct(structFields(id("b"))),
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
                                mergeStruct(structFields(id("b"))),
                                scan(id("bar"), varDecl("b"))
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
     * A list of statements that cannot be converted into the logical algebra yet by [ToLogicalVisitorTransform].  This
     * is temporary--in the near future, we will accomplish this with a better language restriction feature which
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

            // DML
            TodoTestCase("CREATE TABLE foo"),
            TodoTestCase("DROP TABLE foo"),
            TodoTestCase("CREATE INDEX ON foo (x)"),
            TodoTestCase("DROP INDEX bar ON foo"),

            // DDL
            TodoTestCase("INSERT INTO foo VALUE 1"),
            TodoTestCase("INSERT INTO foo VALUE 1"),
            TodoTestCase("FROM x WHERE a = b SET k = 5"),
            TodoTestCase("FROM x INSERT INTO foo VALUES (1, 2)"),
            TodoTestCase("UPDATE x SET k = 5"),
            TodoTestCase("UPDATE x INSERT INTO k << 1 >>"),
            TodoTestCase("DELETE FROM y"),
            TodoTestCase("REMOVE y"),
        )
    }
}
