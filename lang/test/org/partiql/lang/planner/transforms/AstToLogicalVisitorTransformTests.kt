package org.partiql.lang.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.IsGroupAttributeReferenceMeta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.id
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.domains.pathExpr
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.unimplementedProblem
import org.partiql.lang.syntax.PartiQLParser
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.pig.runtime.SymbolPrimitive

/**
 * Test cases in this class might seem a little light--that's because [AstToLogicalVisitorTransform] is getting
 * heavily exercised during many other integration tests.  These should be considered "smoke tests".
 */
class AstToLogicalVisitorTransformTests {
    private val ion = IonSystemBuilder.standard().build()
    internal val parser = PartiQLParser(ion)

    private fun parseAndTransform(sql: String, problemHandler: ProblemHandler): PartiqlLogical.Statement {
        val parseAstStatement = parser.parseAstStatement(sql)
        return parseAstStatement.toLogicalPlan(problemHandler).stmt
    }

    private fun transform(input: PartiqlAst.Statement, problemHandler: ProblemHandler): PartiqlLogical.Statement {
        return input.toLogicalPlan(problemHandler).stmt
    }

    data class TestCase(val original: PartiqlAst.Statement, val expected: PartiqlLogical.Statement) {
        constructor(sql: String, expected: PartiqlLogical.Statement) : this(
            assertDoesNotThrow("Parsing TestCase.sql should not throw") {
                AstToLogicalVisitorTransformTests().parser.parseAstStatement(sql)
            },
            expected
        )
    }

    private fun runTestCase(tc: TestCase) {
        val problemHandler = ProblemCollector()
        val algebra = assertDoesNotThrow("Parsing TestCase.sql should not throw") {
            transform(tc.original, problemHandler)
        }
        assertEquals(
            0,
            problemHandler.problems.filter { it.details.severity == ProblemSeverity.WARNING }.size,
            "No problems were expected"
        )

        assertEquals(tc.expected, algebra)
    }

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToLogicalWindowTests::class)
    fun `to logical (Window)`(tc: TestCase) = runTestCase(tc)

    class ArgumentsForToLogicalWindowTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TestCase(
                // Note:
                // `SELECT * FROM bar AS b` is rewritten to `SELECT b.* FROM bar as b` by [SelectStarVisitorTransform].
                // Therefore, there is no need to support `SELECT *` in `AstToLogicalVisitorTransform`.
                "SELECT lag(a) OVER (PARTITION BY b ORDER BY c) as d FROM bar AS e",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structFields(id("b"))),
                            scan(id("bar"), varDecl("b"))
                        )
                    )
                }
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToLogicalSfwTests::class)
    fun `to logical (SFW)`(tc: TestCase) = runTestCase(tc)

    class ArgumentsForToLogicalSfwTests : ArgumentsProviderBase() {

        private fun PartiqlAst.Builder.simpleGroup(
            projections: List<PartiqlAst.ProjectItem>,
            keys: List<PartiqlAst.GroupKey>,
            groupAsAlias: String? = null,
            fromSource: PartiqlAst.FromSource? = null
        ): PartiqlAst.Statement = query(simpleGroupExpr(projections, keys, groupAsAlias, fromSource))

        private fun PartiqlAst.Builder.simpleGroupExpr(
            projections: List<PartiqlAst.ProjectItem>,
            keys: List<PartiqlAst.GroupKey>,
            groupAsAlias: String? = null,
            fromSource: PartiqlAst.FromSource? = null
        ): PartiqlAst.Expr {
            val from = when (fromSource) {
                null -> {
                    scan(
                        id("bar", caseInsensitive(), unqualified()),
                        asAlias = "b"
                    )
                }
                else -> fromSource
            }
            return select(
                project = projectList(projections),
                from = from,
                group = groupBy(
                    groupFull(),
                    groupKeyList(
                        keys = keys
                    ),
                    groupAsAlias = groupAsAlias
                )
            )
        }

        private fun PartiqlLogical.Builder.simpleAggregateQuery(
            fields: List<PartiqlLogical.StructPart> = emptyList(),
            keys: List<PartiqlLogical.GroupKey> = emptyList(),
            functions: List<PartiqlLogical.AggregateFunction> = emptyList(),
            source: PartiqlLogical.Bexpr? = null
        ): PartiqlLogical.Statement = query(simpleAggregate(fields, keys, functions, source))

        private fun PartiqlLogical.Builder.simpleAggregate(
            fields: List<PartiqlLogical.StructPart> = emptyList(),
            keys: List<PartiqlLogical.GroupKey> = emptyList(),
            functions: List<PartiqlLogical.AggregateFunction> = emptyList(),
            source: PartiqlLogical.Bexpr? = null
        ): PartiqlLogical.Expr {
            val sourceBexpr = when (source) {
                null -> scan(id("bar"), varDecl("b"))
                else -> source
            }
            return bindingsToValues(
                struct(fields),
                aggregate(
                    source = sourceBexpr,
                    strategy = groupFull(),
                    groupList = groupKeyList(keys),
                    functionList = aggregateFunctionList(
                        functions = functions
                    )
                )
            )
        }

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
                                sortSpec(id("y"))
                            )
                        )
                    )
                }
            ),

            // SELECT k AS keyAlias FROM bar AS b GROUP BY y AS k
            TestCase(
                PartiqlAst.build {
                    simpleGroup(
                        projections = listOf(
                            projectExpr(
                                id(
                                    "k",
                                    caseInsensitive(),
                                    unqualified(),
                                    metas = metaContainerOf(IsGroupAttributeReferenceMeta.instance)
                                ),
                                asAlias = "keyAlias"
                            )
                        ),
                        keys = listOf(
                            groupKey_(
                                id("y", caseInsensitive(), unqualified()),
                                asAlias = SymbolPrimitive(
                                    text = "k",
                                    metas = metaContainerOf(UniqueNameMeta("someUniqueName"))
                                )
                            )
                        )
                    )
                },
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("keyAlias")), id("k"))
                        ),
                        keys = listOf(
                            groupKey(
                                id("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("someUniqueName")
                            )
                        )
                    )
                }
            ),

            // SELECT SUM(k) AS keyAlias FROM bar AS b GROUP BY y AS k
            TestCase(
                PartiqlAst.build {
                    simpleGroup(
                        projections = listOf(
                            projectExpr(
                                expr = callAgg(
                                    all(),
                                    "SUM",
                                    id(
                                        "k",
                                        caseInsensitive(),
                                        unqualified(),
                                        metas = metaContainerOf(IsGroupAttributeReferenceMeta.instance)
                                    ),
                                ),
                                asAlias = "keyAlias"
                            )
                        ),
                        keys = listOf(
                            groupKey_(
                                id("y", caseInsensitive(), unqualified()),
                                asAlias = SymbolPrimitive(
                                    text = "k",
                                    metas = metaContainerOf(UniqueNameMeta("someUniqueName"))
                                )
                            )
                        )
                    )
                },
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("keyAlias")), id("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                id("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("someUniqueName")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "SUM",
                                id("k"),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        )
                    )
                }
            ),

            // SELECT SUm(k) AS keyAlias FROM bar AS b
            TestCase(
                PartiqlAst.build {
                    simpleGroup(
                        projections = listOf(
                            projectExpr(
                                expr = callAgg(
                                    all(),
                                    "SUm",
                                    id(
                                        "k",
                                        caseInsensitive(),
                                        unqualified(),
                                        metas = metaContainerOf(IsGroupAttributeReferenceMeta.instance)
                                    ),
                                ),
                                asAlias = "keyAlias"
                            )
                        ),
                        keys = emptyList()
                    )
                },
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("keyAlias")), id("\$__partiql_aggregation_0"))
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "SUm",
                                id("k"),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        )
                    )
                }
            ),

            // SELECT k, g, COUNT(*) AS count FROM bar AS b GROUP BY y AS k GROUP AS g
            TestCase(
                PartiqlAst.build {
                    simpleGroup(
                        projections = listOf(
                            projectExpr(
                                expr = id("k", caseInsensitive(), unqualified(), metas = metaContainerOf(IsGroupAttributeReferenceMeta.instance)),
                                asAlias = "k",
                            ),
                            projectExpr(
                                expr = id("g", caseInsensitive(), unqualified(), metas = metaContainerOf(IsGroupAttributeReferenceMeta.instance)),
                                asAlias = "g"
                            ),
                            projectExpr(
                                expr = callAgg(
                                    all(),
                                    "COUNT",
                                    lit(ionInt(1))
                                ),
                                asAlias = "count"
                            )
                        ),
                        keys = listOf(
                            groupKey_(
                                id("y", caseInsensitive(), unqualified()),
                                asAlias = SymbolPrimitive(
                                    text = "k",
                                    metas = metaContainerOf(UniqueNameMeta("someUniqueName"))
                                )
                            )
                        ),
                        groupAsAlias = "g"
                    )
                },
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("k")), id("k")),
                            structField(lit(ionSymbol("g")), id("g")),
                            structField(lit(ionSymbol("count")), id("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                id("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("someUniqueName")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "group_as",
                                struct(
                                    structField(
                                        lit(ionSymbol("b")),
                                        id("b")
                                    )
                                ),
                                varDecl("g")
                            ),
                            aggregateFunction(
                                all(),
                                "COUNT",
                                lit(ionInt(1)),
                                varDecl("\$__partiql_aggregation_0")
                            ),
                        )
                    )
                }
            ),

            // SELECT k, MAX(a) AS max FROM (SELECT k, MAX(a) FROM bar AS b GROUP BY c AS k) AS b GROUP BY y AS k
            TestCase(
                PartiqlAst.build {
                    val innerQuery = simpleGroupExpr(
                        projections = listOf(
                            projectExpr(
                                expr = id("k", caseInsensitive(), unqualified(), metas = metaContainerOf(IsGroupAttributeReferenceMeta.instance)),
                                asAlias = "k",
                            ),
                            projectExpr(
                                expr = callAgg(
                                    all(),
                                    "MAX",
                                    id("a", caseInsensitive(), unqualified())
                                ),
                                asAlias = "max"
                            )
                        ),
                        keys = listOf(
                            groupKey_(
                                id("y", caseInsensitive(), unqualified()),
                                asAlias = SymbolPrimitive(
                                    text = "k",
                                    metas = metaContainerOf(UniqueNameMeta("someUniqueName"))
                                )
                            )
                        )
                    )
                    simpleGroup(
                        projections = listOf(
                            projectExpr(
                                expr = id("k", caseInsensitive(), unqualified(), metas = metaContainerOf(IsGroupAttributeReferenceMeta.instance)),
                                asAlias = "k",
                            ),
                            projectExpr(
                                expr = callAgg(
                                    all(),
                                    "MAX",
                                    id("a", caseInsensitive(), unqualified())
                                ),
                                asAlias = "max"
                            )
                        ),
                        keys = listOf(
                            groupKey_(
                                id("y", caseInsensitive(), unqualified()),
                                asAlias = SymbolPrimitive(
                                    text = "k",
                                    metas = metaContainerOf(UniqueNameMeta("someUniqueName"))
                                )
                            )
                        ),
                        fromSource = scan(innerQuery, asAlias = "b")
                    )
                },
                PartiqlLogical.build {
                    val innerQuery = simpleAggregate(
                        fields = listOf(
                            structField(lit(ionSymbol("k")), id("k")),
                            structField(lit(ionSymbol("max")), id("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                id("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("someUniqueName")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "MAX",
                                id("a", caseInsensitive(), unqualified()),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        )
                    )
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("k")), id("k")),
                            structField(lit(ionSymbol("max")), id("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                id("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("someUniqueName")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "MAX",
                                id("a", caseInsensitive(), unqualified()),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        ),
                        source = scan(innerQuery, varDecl("b"))
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
    fun `unimplemented features are blocked`(tc: ProblemTestCase) {
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
