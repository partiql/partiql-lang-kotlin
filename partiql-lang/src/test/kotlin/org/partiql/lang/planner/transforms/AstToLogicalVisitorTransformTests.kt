package org.partiql.lang.planner.transforms

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
import org.partiql.errors.Problem
import org.partiql.errors.ProblemHandler
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.pathExpr
import org.partiql.lang.domains.vr
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.eval.builtins.ExprFunctionCurrentUser
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.unimplementedProblem
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.util.ArgumentsProviderBase

/**
 * Test cases in this class might seem a little light--that's because [AstToLogicalVisitorTransform] is getting
 * heavily exercised during many other integration tests.  These should be considered "smoke tests".
 */
class AstToLogicalVisitorTransformTests {
    internal val parser = PartiQLParserBuilder.standard().build()

    private fun parseAndTransform(sql: String, problemHandler: ProblemHandler): PartiqlLogical.Statement {
        val parseAstStatement = parser.parseAstStatement(sql)
        return parseAstStatement.toLogicalPlan(problemHandler).stmt
    }

    private fun transform(input: PartiqlAst.Statement, problemHandler: ProblemHandler): PartiqlLogical.Statement {
        return input.toLogicalPlan(problemHandler).stmt
    }

    data class TestCase(val original: PartiqlAst.Statement, val expected: PartiqlLogical.Statement, val originalSql: String? = null) {
        constructor(sql: String, expected: PartiqlLogical.Statement) : this(
            assertDoesNotThrow("Parsing TestCase.sql should not throw") {
                AstToLogicalVisitorTransformTests().parser.parseAstStatement(sql)
            },
            expected,
            sql
        )

        override fun toString(): String {
            return when (this.originalSql) {
                null -> this.original.toString()
                else -> this.originalSql
            }
        }
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
                "SELECT lag(a) OVER (ORDER BY b) as c FROM d AS e",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structField(lit(ionSymbol("c")), vr("\$__partiql_window_function_0"))),
                            window(
                                scan(vr("d"), varDecl("e")),
                                over(
                                    null,
                                    windowSortSpecList(sortSpec(vr("b"), null, null)),
                                ),
                                windowExpression(varDecl("\$__partiql_window_function_0"), "lag", listOf(vr("a")))
                            )
                        )
                    )
                }
            ),
            // Multiple Window Expression
            TestCase(
                "SELECT lag(a) OVER (ORDER BY b) as c , lead(a) OVER (ORDER BY b) as f FROM d AS e",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(
                                structField(lit(ionSymbol("c")), vr("\$__partiql_window_function_0")),
                                structField(lit(ionSymbol("f")), vr("\$__partiql_window_function_1")),
                            ),
                            window(
                                window(
                                    scan(vr("d"), varDecl("e")),
                                    over(
                                        null,
                                        windowSortSpecList(sortSpec(vr("b"), null, null)),
                                    ),
                                    windowExpression(varDecl("\$__partiql_window_function_0"), "lag", listOf(vr("a")))
                                ),
                                over(
                                    null,
                                    windowSortSpecList(sortSpec(vr("b"), null, null)),
                                ),
                                windowExpression(varDecl("\$__partiql_window_function_1"), "lead", listOf(vr("a")))
                            )
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
            groupAsAlias: PartiqlAst.Defnid? = null,
            fromSource: PartiqlAst.FromSource? = null
        ): PartiqlAst.Statement = query(simpleGroupExpr(projections, keys, groupAsAlias, fromSource))

        private fun PartiqlAst.Builder.simpleGroupExpr(
            projections: List<PartiqlAst.ProjectItem>,
            keys: List<PartiqlAst.GroupKey>,
            groupAsAlias: PartiqlAst.Defnid? = null,
            fromSource: PartiqlAst.FromSource? = null
        ): PartiqlAst.Expr {
            val from = when (fromSource) {
                null -> {
                    scan(
                        vr("bar", caseInsensitive(), unqualified()),
                        asAlias = defnid("b")
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
                null -> scan(vr("bar"), varDecl("b"))
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

        private fun PartiqlAst.Builder.simpleHaving(
            projections: List<PartiqlAst.ProjectItem>,
            keys: List<PartiqlAst.GroupKey>,
            groupAsAlias: PartiqlAst.Defnid? = null,
            fromSource: PartiqlAst.FromSource? = null,
            having: PartiqlAst.Expr
        ): PartiqlAst.Statement = query(simpleHavingExpr(projections, keys, groupAsAlias, fromSource, having))

        private fun PartiqlAst.Builder.simpleHavingExpr(
            projections: List<PartiqlAst.ProjectItem>,
            keys: List<PartiqlAst.GroupKey>,
            groupAsAlias: PartiqlAst.Defnid? = null,
            fromSource: PartiqlAst.FromSource? = null,
            having: PartiqlAst.Expr
        ): PartiqlAst.Expr {
            val from = when (fromSource) {
                null -> {
                    scan(
                        vr("bar", caseInsensitive(), unqualified()),
                        asAlias = defnid("b")
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
                ),
                having = having
            )
        }

        private fun PartiqlLogical.Builder.simpleHavingLogicalQuery(
            fields: List<PartiqlLogical.StructPart> = emptyList(),
            keys: List<PartiqlLogical.GroupKey> = emptyList(),
            functions: List<PartiqlLogical.AggregateFunction> = emptyList(),
            source: PartiqlLogical.Bexpr? = null,
            predicate: PartiqlLogical.Expr
        ): PartiqlLogical.Statement = query(simpleHavingLogical(fields, keys, functions, source, predicate))

        private fun PartiqlLogical.Builder.simpleHavingLogical(
            fields: List<PartiqlLogical.StructPart> = emptyList(),
            keys: List<PartiqlLogical.GroupKey> = emptyList(),
            functions: List<PartiqlLogical.AggregateFunction> = emptyList(),
            source: PartiqlLogical.Bexpr? = null,
            predicate: PartiqlLogical.Expr
        ): PartiqlLogical.Expr {
            val sourceBexpr = when (source) {
                null -> scan(vr("bar"), varDecl("b"))
                else -> source
            }
            return bindingsToValues(
                struct(fields),
                filter(
                    predicate = predicate,
                    source = aggregate(
                        source = sourceBexpr,
                        strategy = groupFull(),
                        groupList = groupKeyList(keys),
                        functionList = aggregateFunctionList(
                            functions = functions
                        )
                    )
                )
            )
        }

        override fun getParameters() = listOf(
            TestCase(
                "CURRENT_USER",
                PartiqlLogical.build {
                    query(
                        call(
                            defnid(ExprFunctionCurrentUser.FUNCTION_NAME),
                            emptyList()
                        )
                    )
                }
            ),
            TestCase(
                "CURRENT_USER || 'hello'",
                PartiqlLogical.build {
                    query(
                        concat(
                            listOf(
                                call(
                                    defnid(ExprFunctionCurrentUser.FUNCTION_NAME),
                                    emptyList()
                                ),
                                lit(
                                    ionString("hello")
                                )
                            )
                        )
                    )
                }
            ),
            TestCase(
                // Note:
                // `SELECT * FROM bar AS b` is rewritten to `SELECT b.* FROM bar as b` by [SelectStarVisitorTransform].
                // Therefore, there is no need to support `SELECT *` in `AstToLogicalVisitorTransform`.
                "SELECT b.* FROM bar AS b",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structFields(vr("b"))),
                            scan(vr("bar"), varDecl("b"))
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
                            struct(structFields(vr("b"))),
                            filter(
                                eq(lit(ionBool(true)), lit(ionBool(true))),
                                scan(vr("bar"), varDecl("b"))
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
                            struct(structFields(vr("b"))),
                            filter(
                                eq(path(vr("b"), pathExpr(lit(ionString("primaryKey")))), lit(ionInt(42))),
                                scan(vr("bar"), varDecl("b"))
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
                            defnid("filter_distinct"),
                            bindingsToValues(
                                struct(structFields(vr("b"))),
                                scan(vr("bar"), varDecl("b"))
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
                                structFields(vr("v")),
                                structFields(vr("n"))
                            ),
                            unpivot(vr("bar"), varDecl("v"), varDecl("n"))
                        )
                    )
                }
            ),
            TestCase(
                "SELECT b.* FROM bar AS b ORDER BY y",
                PartiqlLogical.build {
                    query(
                        bindingsToValues(
                            struct(structFields(vr("b"))),
                            sort(
                                scan(vr("bar"), varDecl("b")),
                                sortSpec(vr("y"))
                            )
                        )
                    )
                }
            ),

            TestCase(
                sql = """
                    SELECT k AS keyAlias
                    FROM bar AS b
                    GROUP BY y AS k
                """,
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("keyAlias")), vr("k"))
                        ),
                        keys = listOf(
                            groupKey(
                                vr("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("k")
                            )
                        )
                    )
                }
            ),

            TestCase(
                sql = """
                    SELECT SUM(k) AS sum_k
                    FROM bar AS b
                    GROUP BY y AS k
                """,
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("sum_k")), vr("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                vr("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("k")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "sum",
                                vr("k"),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        )
                    )
                }
            ),

            TestCase(
                sql = """
                    SELECT SUm(k) AS sum_k
                    FROM bar AS b
                """,
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("sum_k")), vr("\$__partiql_aggregation_0"))
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "sum",
                                vr("k"),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        )
                    )
                }
            ),

            TestCase(
                sql = """
                    SELECT k AS k, g AS g, COUNT(*) AS ct
                    FROM bar AS b
                    GROUP BY y AS k
                    GROUP AS g
                """,
                PartiqlLogical.build {
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("k")), vr("k")),
                            structField(lit(ionSymbol("g")), vr("g")),
                            structField(lit(ionSymbol("ct")), vr("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                vr("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("k")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "group_as",
                                struct(
                                    structField(
                                        lit(ionSymbol("b")),
                                        vr("b")
                                    )
                                ),
                                varDecl("g")
                            ),
                            aggregateFunction(
                                all(),
                                "count",
                                lit(ionInt(1)),
                                varDecl("\$__partiql_aggregation_0")
                            ),
                        )
                    )
                }
            ),

            TestCase(
                sql = """
                    SELECT k AS k, MAX(a) AS mx
                    FROM
                        (
                            SELECT k AS k, MAX(a) AS mx
                            FROM bar AS b
                            GROUP BY c AS k
                        ) AS b
                    GROUP BY y AS k
                """,
                PartiqlLogical.build {
                    val innerQuery = simpleAggregate(
                        fields = listOf(
                            structField(lit(ionSymbol("k")), vr("k")),
                            structField(lit(ionSymbol("mx")), vr("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                vr("c", caseInsensitive(), unqualified()),
                                asVar = varDecl("k")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "max",
                                vr("a", caseInsensitive(), unqualified()),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        )
                    )
                    simpleAggregateQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("k")), vr("k")),
                            structField(lit(ionSymbol("mx")), vr("\$__partiql_aggregation_0"))
                        ),
                        keys = listOf(
                            groupKey(
                                vr("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("k")
                            )
                        ),
                        functions = listOf(
                            aggregateFunction(
                                all(),
                                "max",
                                vr("a", caseInsensitive(), unqualified()),
                                varDecl("\$__partiql_aggregation_0")
                            )
                        ),
                        source = scan(innerQuery, varDecl("b"))
                    )
                }
            ),

            TestCase(
                sql = """
                    SELECT k AS keyAlias
                    FROM bar AS b
                    GROUP BY y AS k
                    HAVING k > 2
                """,
                PartiqlLogical.build {
                    simpleHavingLogicalQuery(
                        fields = listOf(
                            structField(lit(ionSymbol("keyAlias")), vr("k"))
                        ),
                        keys = listOf(
                            groupKey(
                                vr("y", caseInsensitive(), unqualified()),
                                asVar = varDecl("k")
                            )
                        ),
                        predicate = gt(vr("k"), lit(ionInt(2)))
                    )
                }
            ),

            TestCase(
                sql = """
                    SUM(1)
                """,
                PartiqlLogical.build {
                    query(
                        call(
                            defnid("coll_sum"),
                            listOf(
                                lit(ionString("all")),
                                lit(ionInt(1))
                            )
                        )
                    )
                }
            ),
            TestCase(
                sql = """
                    SUM(DISTINCT 1)
                """,
                PartiqlLogical.build {
                    query(
                        call(
                            defnid("coll_sum"),
                            listOf(
                                lit(ionString("distinct")),
                                lit(ionInt(1))
                            )
                        )
                    )
                }
            ),
            TestCase(
                sql = """
                    SELECT SUM(a) AS sum_a
                    FROM t AS t
                    LET SUM(c) AS sum_c
                    WHERE SUM(ALL b)
                    GROUP BY d AS e
                    HAVING AVG(ALL f)
                    ORDER BY MIN(DISTINCT g)
                    LIMIT SUM(DISTINCT 2)
                """,
                PartiqlLogical.build {
                    val scan = scan(vr("t"), varDecl("t"))
                    val let = let(scan, letBinding(call(defnid("coll_sum"), listOf(lit(ionString("all")), vr("c"))), varDecl("sum_c")))
                    val where = filter(call(defnid("coll_sum"), listOf(lit(ionString("all")), vr("b"))), let)
                    val agg = aggregate(
                        where,
                        groupFull(),
                        groupKeyList(groupKey(vr("d"), varDecl("e"))),
                        aggregateFunctionList(
                            aggregateFunction(all(), "sum", vr("a"), varDecl("\$__partiql_aggregation_0")),
                            aggregateFunction(all(), "avg", vr("f"), varDecl("\$__partiql_aggregation_1")),
                            aggregateFunction(distinct(), "min", vr("g"), varDecl("\$__partiql_aggregation_2")),
                        )
                    )
                    val having = filter(vr("\$__partiql_aggregation_1"), agg)
                    val order = sort(having, sortSpec(vr("\$__partiql_aggregation_2")))
                    val limit = limit(call(defnid("coll_sum"), listOf(lit(ionString("distinct")), lit(ionInt(2)))), order)
                    val projection = bindingsToValues(struct(structField(lit(ionSymbol("sum_a")), vr("\$__partiql_aggregation_0"))), limit)
                    query(projection)
                }
            ),
            TestCase(
                sql = """
                    SELECT SUM(a) + COLL_COUNT('distinct', a) AS sum_a
                    FROM t AS t
                """,
                PartiqlLogical.build {
                    val scan = scan(vr("t"), varDecl("t"))
                    val agg = aggregate(
                        scan,
                        groupFull(),
                        groupKeyList(emptyList()),
                        aggregateFunctionList(
                            aggregateFunction(all(), "sum", vr("a"), varDecl("\$__partiql_aggregation_0")),
                        )
                    )
                    val expression = plus(
                        vr("\$__partiql_aggregation_0"),
                        call(defnid("coll_count"), listOf(lit(ionString("distinct")), vr("a")))
                    )
                    val projection = bindingsToValues(struct(structField(lit(ionSymbol("sum_a")), expression)), agg)
                    query(projection)
                }
            ),
            TestCase(
                sql = """
                    SELECT (
                        SELECT SUM(a) + COLL_COUNT('distinct', b) AS agg_proj
                        FROM t1 AS t1
                    ) AS inner_query
                    FROM (
                        SELECT AVG(c) + COLL_MAX('all', d) AS agg_from
                        FROM t2 AS t2
                    ) AS src
                """,
                PartiqlLogical.build {
                    // Create Sub-Query in PROJECTION
                    val scanProj = scan(vr("t1"), varDecl("t1"))
                    val aggProj = aggregate(
                        scanProj,
                        groupFull(),
                        groupKeyList(emptyList()),
                        aggregateFunctionList(
                            aggregateFunction(all(), "sum", vr("a"), varDecl("\$__partiql_aggregation_0")),
                        )
                    )
                    val exprProj = plus(
                        vr("\$__partiql_aggregation_0"),
                        call(defnid("coll_count"), listOf(lit(ionString("distinct")), vr("b")))
                    )
                    val bindingsProj = bindingsToValues(struct(structField(lit(ionSymbol("agg_proj")), exprProj)), aggProj)

                    // Create Sub-Query in FROM
                    val scanFrom = scan(vr("t2"), varDecl("t2"))
                    val aggFrom = aggregate(
                        scanFrom,
                        groupFull(),
                        groupKeyList(emptyList()),
                        aggregateFunctionList(
                            aggregateFunction(all(), "avg", vr("c"), varDecl("\$__partiql_aggregation_0")),
                        )
                    )
                    val exprFrom = plus(
                        vr("\$__partiql_aggregation_0"),
                        call(defnid("coll_max"), listOf(lit(ionString("all")), vr("d")))
                    )
                    val bindingsFrom = bindingsToValues(struct(structField(lit(ionSymbol("agg_from")), exprFrom)), aggFrom)

                    // Create Full Query
                    val scanOuter = scan(bindingsFrom, varDecl("src"))
                    val projection = bindingsToValues(struct(structField(lit(ionSymbol("inner_query")), bindingsProj)), scanOuter)
                    query(projection)
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
                        id("foo", caseInsensitive()),
                        dmlInsert(varDecl("foo")),
                        bag(lit(ionInt(1)))
                    )
                }
            ),

            TestCase(
                "INSERT INTO foo SELECT x.* FROM 1 AS x",
                PartiqlLogical.build {
                    dml(
                        id("foo", caseInsensitive()),
                        dmlInsert(varDecl("foo")),
                        bindingsToValues(
                            struct(structFields(vr("x", caseInsensitive(), unqualified()))),
                            scan(lit(ionInt(1)), varDecl("x"))
                        )
                    )
                }
            ),
            TestCase(
                "INSERT INTO foo SELECT x.* FROM 1 AS x ON CONFLICT DO REPLACE EXCLUDED",
                PartiqlLogical.build {
                    dml(
                        id("foo", caseInsensitive()),
                        dmlReplace(varDecl("foo")),
                        bindingsToValues(
                            struct(structFields(vr("x", caseInsensitive(), unqualified()))),
                            scan(lit(ionInt(1)), varDecl("x"))
                        )
                    )
                }
            ),
            TestCase(
                "INSERT INTO foo SELECT x.* FROM 1 AS x ON CONFLICT DO REPLACE EXCLUDED WHERE foo.id > 2",
                PartiqlLogical.build {
                    dml(
                        id("foo", caseInsensitive()),
                        dmlReplace(
                            targetAlias = varDecl("foo"),
                            condition = gt(
                                listOf(
                                    path(
                                        vr("foo", caseInsensitive(), unqualified()),
                                        listOf(pathExpr(lit(ionString("id")), caseInsensitive()))
                                    ),
                                    lit(ionInt(2))
                                )
                            ),
                            rowAlias = varDecl(AstToLogicalVisitorTransform.EXCLUDED)
                        ),
                        bindingsToValues(
                            struct(structFields(vr("x", caseInsensitive(), unqualified()))),
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
                            id("foo", caseInsensitive()),
                            dmlReplace(varDecl("f")),
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
                "INSERT INTO foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED WHERE f.id > 2",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            id("foo", caseInsensitive()),
                            dmlReplace(
                                varDecl("f"),
                                condition = gt(
                                    listOf(
                                        path(
                                            vr("f", caseInsensitive(), unqualified()),
                                            listOf(pathExpr(lit(ionString("id")), caseInsensitive()))
                                        ),
                                        lit(ionInt(2))
                                    )
                                ),
                                rowAlias = varDecl(AstToLogicalVisitorTransform.EXCLUDED)
                            ),
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

            // A collection of values
            TestCase(
                "INSERT INTO foo AS f <<{'id': 1, 'name':'bob'}, {'id': 2, 'name':'alice'}>> ON CONFLICT DO REPLACE EXCLUDED WHERE f.id > 2",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            id("foo", caseInsensitive()),
                            dmlReplace(
                                varDecl("f"),
                                condition = gt(
                                    listOf(
                                        path(
                                            vr("f", caseInsensitive(), unqualified()),
                                            listOf(pathExpr(lit(ionString("id")), caseInsensitive()))
                                        ),
                                        lit(ionInt(2))
                                    )
                                ),
                                rowAlias = varDecl(AstToLogicalVisitorTransform.EXCLUDED)
                            ),
                            bag(
                                struct(
                                    structField(lit(ionString("id")), lit(ionInt(1))),
                                    structField(lit(ionString("name")), lit(ionString("bob")))
                                ),
                                struct(
                                    structField(lit(ionString("id")), lit(ionInt(2))),
                                    structField(lit(ionString("name")), lit(ionString("alice")))
                                )
                            )
                        )
                    }
                }
            ),
            // Testing using excluded non-reserved keyword in condition
            TestCase(
                "INSERT INTO foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED WHERE excluded.id > 2",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            id("foo", caseInsensitive()),
                            dmlReplace(
                                varDecl("f"),
                                condition = gt(
                                    listOf(
                                        path(
                                            vr("excluded", caseInsensitive(), unqualified()),
                                            listOf(pathExpr(lit(ionString("id")), caseInsensitive()))
                                        ),
                                        lit(ionInt(2))
                                    )
                                ),
                                rowAlias = varDecl(AstToLogicalVisitorTransform.EXCLUDED)
                            ),
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
                        id("foo", caseInsensitive()),
                        dmlUpdate(varDecl("foo")),
                        bindingsToValues(
                            struct(structFields(vr("x", caseInsensitive(), unqualified()))),
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
                            id("foo", caseInsensitive()),
                            dmlUpdate(varDecl("f")),
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
                "INSERT INTO foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED WHERE f.id > 2",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            id("foo", caseInsensitive()),
                            dmlUpdate(
                                varDecl("f"),
                                condition = gt(
                                    listOf(
                                        path(
                                            vr("f", caseInsensitive(), unqualified()),
                                            listOf(pathExpr(lit(ionString("id")), caseInsensitive()))
                                        ),
                                        lit(ionInt(2))
                                    )
                                ),
                                rowAlias = varDecl(AstToLogicalVisitorTransform.EXCLUDED)
                            ),
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
            // Testing using excluded non-reserved keyword in condition
            TestCase(
                "INSERT INTO foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED WHERE excluded.id > 2",
                PartiqlLogical.build {
                    PartiqlLogical.build {
                        dml(
                            id("foo", caseInsensitive()),
                            dmlUpdate(
                                varDecl("f"),
                                condition = gt(
                                    listOf(
                                        path(
                                            vr("excluded", caseInsensitive(), unqualified()),
                                            listOf(pathExpr(lit(ionString("id")), caseInsensitive()))
                                        ),
                                        lit(ionInt(2))
                                    )
                                ),
                                rowAlias = varDecl(AstToLogicalVisitorTransform.EXCLUDED)
                            ),
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
                            id("foo", caseInsensitive()),
                            dmlReplace(varDecl("f")),
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
                            id("foo", caseInsensitive()),
                            dmlUpdate(varDecl("f")),
                            bindingsToValues(
                                struct(structFields(vr("x", caseInsensitive(), unqualified()))),
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
                        id("y", caseInsensitive()),
                        dmlDelete(),
                        bindingsToValues(
                            vr("y", caseSensitive(), unqualified()),
                            scan(vr("y", caseInsensitive(), unqualified()), varDecl("y"))
                        )
                    )
                }
            ),
            TestCase(
                "DELETE FROM y AS y WHERE 1=1",
                PartiqlLogical.build {
                    dml(
                        id("y", caseInsensitive()),
                        dmlDelete(),
                        bindingsToValues(
                            vr("y", caseSensitive(), unqualified()),
                            // this logical plan is same as previous but includes this filter
                            filter(
                                eq(lit(ionInt(1)), lit(ionInt(1))),
                                scan(vr("y", caseInsensitive(), unqualified()), varDecl("y"))
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
                            key = path(vr("x"), listOf(pathExpr(lit(ionString("v"))))),
                            value = path(vr("x"), listOf(pathExpr(lit(ionString("a"))))),
                        )
                    )
                }
            )
        )
    }

    data class ProblemTestCase(val id: Int, val sql: String, val expectedProblem: Problem)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForProblemTests::class)
    fun `unimplemented features are blocked`(tc: ProblemTestCase) {
        val id by lazy { "[in test case #${tc.id}]" }
        val problemHandler = ProblemCollector()
        assertDoesNotThrow("Parsing TestCase.sql should not throw $id") {
            parseAndTransform(tc.sql, problemHandler)
        }

        assertFalse(problemHandler.hasWarnings, "didn't expect any warnings $id")
        assertTrue(problemHandler.hasErrors, "at least one error was expected $id")

        assertEquals(tc.expectedProblem, problemHandler.problems.first(), "actual problem is not as expected $id")
    }

    /**
     * Below are all statements that cannot be converted into the logical algebra yet by [AstToLogicalVisitorTransform].
     * This is temporary--in the near future, we will accomplish this with a better language restriction feature which
     * blocks all language features except those explicitly allowed.  This will be needed to constrain possible queries
     * to features supported by specific PartiQL-services.
     */
    class ArgumentsForProblemTests : ArgumentsProviderBase() {

        override fun getParameters() = listOf(
            // DDL is  not implemented
            ProblemTestCase(100, "CREATE TABLE foo (boo string)", unimplementedProblem("CREATE TABLE", 1, 1, 6)),
            ProblemTestCase(101, "DROP TABLE foo", unimplementedProblem("DROP TABLE", 1, 1, 4)),
            ProblemTestCase(102, "CREATE INDEX ON foo (x)", unimplementedProblem("CREATE INDEX", 1, 1, 6)),
            ProblemTestCase(103, "DROP INDEX bar ON foo", unimplementedProblem("DROP INDEX", 1, 1, 4)),

            // Unimplemented parts of DML
            ProblemTestCase(200, "FROM x AS xx INSERT INTO foo VALUES (1, 2)", unimplementedProblem("UPDATE / INSERT", 1, 14, 6)),
            ProblemTestCase(201, "FROM x AS xx SET k = 5", unimplementedProblem("SET", 1, 14, 3)),
            ProblemTestCase(202, "UPDATE x SET k = 5", unimplementedProblem("SET", 1, 10, 3)),
            ProblemTestCase(203, "UPDATE x REMOVE k", unimplementedProblem("REMOVE", 1, 10, 6)),
            ProblemTestCase(204, "UPDATE x INSERT INTO k << 1 >>", unimplementedProblem("UPDATE / INSERT", 1, 10, 6)),

            // INSERT INTO ... VALUE ... is not supported because it is redundant with INSERT INTO ... << <expr> >>
            ProblemTestCase(
                300,
                "INSERT INTO x VALUE 1",
                Problem(ProblemLocation(1, 1, 6), PlanningProblemDetails.InsertValueDisallowed)
            ),
            // We need schema to support using INSERT INTO without an explicit list of fields.
            ProblemTestCase(
                301,
                "INSERT INTO x VALUES (1, 2, 3)",
                Problem(ProblemLocation(1, 1, 6), PlanningProblemDetails.InsertValuesDisallowed)
            )
        )
    }
}
