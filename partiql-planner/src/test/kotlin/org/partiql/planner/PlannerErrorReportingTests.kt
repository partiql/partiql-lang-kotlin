package org.partiql.planner

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

internal class PlannerErrorReportingTests {
    val catalogName = "mode_test"
    val userId = "test-user"
    val queryId = "query"

    val catalog = MemoryCatalog
        .PartiQL()
        .name(catalogName)
        .define("missing_binding", StaticType.MISSING)
        .define("atomic", StaticType.INT2)
        .define("collection_no_missing_atomic", BagType(StaticType.INT2))
        .define("collection_contain_missing_atomic", BagType(StaticType.unionOf(StaticType.INT2, StaticType.MISSING)))
        .define("struct_no_missing", closedStruct(StructType.Field("f1", StaticType.INT2)))
        .define(
            "struct_with_missing",
            closedStruct(
                StructType.Field("f1", StaticType.unionOf(StaticType.INT2, StaticType.MISSING)),
                StructType.Field("f2", StaticType.MISSING),
            )
        )
        .build()

    val metadata = MemoryConnector(catalog).getMetadata(
        object : ConnectorSession {
            override fun getQueryId(): String = "q"
            override fun getUserId(): String = "s"
        }
    )

    val session = PartiQLPlanner.Session(
        queryId = queryId,
        userId = userId,
        currentCatalog = catalogName,
        catalogs = mapOf(catalogName to metadata),
    )

    val parser = PartiQLParserBuilder().build()

    val statement: ((String) -> Statement) = { query ->
        parser.parse(query).root
    }

    fun assertProblem(
        plan: org.partiql.plan.PlanNode,
        problems: List<Problem>,
        vararg block: () -> Boolean
    ) {
        block.forEachIndexed { index, function ->
            assert(function.invoke()) {
                buildString {
                    this.appendLine("assertion #${index + 1} failed")

                    this.appendLine("--------Plan---------")
                    PlanPrinter.append(this, plan)

                    this.appendLine("----------Problems---------")
                    problems.forEach {
                        this.appendLine(it.toString())
                    }
                }
            }
        }
    }

    data class TestCase(
        val query: String,
        val isSignal: Boolean,
        val assertion: (List<Problem>) -> List<() -> Boolean>,
        val expectedType: StaticType = StaticType.MISSING
    )

    companion object {
        fun closedStruct(vararg field: StructType.Field): StructType =
            StructType(
                field.toList(),
                contentClosed = true,
                emptyList(),
                setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )

        private fun assertOnProblemCount(warningCount: Int, errorCount: Int): (List<Problem>) -> List<() -> Boolean> = { problems ->
            listOf(
                { problems.filter { it.details.severity == ProblemSeverity.WARNING }.size == warningCount },
                { problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == errorCount },
            )
        }

        /**
         * Those tests focus on MissingOpBehavior.
         */
        @JvmStatic
        fun testProblems() = listOf(
            // Literal MISSING Does not throw warnings or errors in either mode.
            TestCase(
                "MISSING",
                false,
                assertOnProblemCount(0, 0)
            ),
            TestCase(
                "MISSING",
                true,
                assertOnProblemCount(0, 0)
            ),
            // Unresolved variable always signals (10.1.3)
            TestCase(
                "var_not_exist",
                true,
                assertOnProblemCount(0, 1)
            ),

            // Function propagates missing in quite mode
            TestCase(
                "1 + MISSING",
                false,
                assertOnProblemCount(1, 0)
            ),
            // This will be a non-resolved function error.
            // As plus does not contain a function that match argument type with
            // int32 and missing.
            //  Error in signaling mode.
            TestCase(
                "1 + MISSING",
                true,
                assertOnProblemCount(0, 1)
            ),
            // Attempting to do path navigation(symbol) on missing(which is not tuple)
            //  returns missing in quite mode, and error out in signal mode
            TestCase(
                "MISSING.a",
                false,
                assertOnProblemCount(1, 0)
            ),
            TestCase(
                "MISSING.a",
                true,
                assertOnProblemCount(0, 1)
            ),
            // Attempting to do path navigation(index) on missing(which is not list)
            //  returns missing in quite mode, and error out in signal mode
            TestCase(
                "MISSING[1]",
                false,
                assertOnProblemCount(1, 0)
            ),
            TestCase(
                "MISSING[1]",
                true,
                assertOnProblemCount(0, 1)
            ),
            // Attempting to do path navigation(key) on missing(which is tuple)
            //  returns missing in quite mode, and error out in signal mode
            TestCase(
                "MISSING['a']",
                false,
                assertOnProblemCount(1, 0)
            ),
            TestCase(
                "MISSING['a']",
                true,
                assertOnProblemCount(0, 1)
            ),
            // Chained, demostrate missing trace.
            TestCase(
                "MISSING['a'].a",
                false,
                assertOnProblemCount(2, 0)
            ),
            TestCase(
                "MISSING['a'].a",
                true,
                assertOnProblemCount(0, 2)
            ),
            TestCase(
                """
                    -- one branch is missing, no problem
                    CASE WHEN   
                        1 = 1 THEN MISSING
                        ELSE 2 END
                """.trimIndent(),
                false,
                assertOnProblemCount(0, 0),
                StaticType.unionOf(StaticType.INT4, StaticType.MISSING)
            ),
            TestCase(
                """
                    -- one branch is missing, no problem
                    CASE WHEN 
                        1 = 1 THEN MISSING
                        ELSE 2 END
                """.trimIndent(),
                true,
                assertOnProblemCount(0, 0),
                StaticType.unionOf(StaticType.INT4, StaticType.MISSING)
            ),
            TestCase(
                """
                    -- both branches are missing, problem
                    CASE WHEN
                        1 = 1 THEN MISSING
                        ELSE MISSING END
                """.trimIndent(),
                false,
                assertOnProblemCount(1, 0),
            ),
            TestCase(
                """
                    -- both branches are missing, problem
                    CASE WHEN  
                        1 = 1 THEN MISSING
                        ELSE MISSING END
                """.trimIndent(),
                true,
                assertOnProblemCount(0, 1),
            ),
        )

        /**
         * Those tests focus on continuation
         */
        @JvmStatic
        fun testContinuation() = listOf(
            // Continuation with data type mismatch
            // the expected type for this case is missing.
            //  as we know for sure that a + b returns missing.
            TestCase(
                " 'a' + 'b' ",
                false,
                assertOnProblemCount(1, 0),
                StaticType.MISSING
            ),
            TestCase(
                " 'a' + 'b' ",
                true,
                assertOnProblemCount(0, 1),
                StaticType.MISSING
            ),

            // No function with given name is registered.
            // always going to return error regardless of mode.
            // The expected type for continuation is ANY.
            TestCase(
                "not_a_function(1)",
                false,
                assertOnProblemCount(0, 1),
                StaticType.MISSING
            ),
            TestCase(
                "not_a_function(1)",
                true,
                assertOnProblemCount(0, 1),
                StaticType.MISSING
            ),

            // 1 + not_a_function(1)
            //  The continuation will return all numeric type
            TestCase(
                "1 + not_a_function(1)",
                false,
                assertOnProblemCount(1, 1),
                StaticType.MISSING,
            ),
            TestCase(
                "1 + not_a_function(1)",
                false,
                assertOnProblemCount(1, 1),
                StaticType.MISSING,
            ),

            TestCase(
                """
                    SELECT 
                        t.f1, -- SUCCESS
                        t.f2 -- no such field
                        FROM struct_no_missing as t
                """.trimIndent(),
                false,
                assertOnProblemCount(1, 0),
                BagType(closedStruct(StructType.Field("f1", StaticType.INT2)))
            ),
            TestCase(
                """
                    SELECT 
                        t.f1, -- SUCCESS
                        t.f2 -- no such field
                        FROM struct_no_missing as t
                """.trimIndent(),
                true,
                assertOnProblemCount(0, 1),
                BagType(closedStruct(StructType.Field("f1", StaticType.INT2)))
            ),
            TestCase(
                """
                    SELECT 
                        t.f1, -- OK 
                        t.f2, -- always missing
                        t.f3 -- no such field
                    FROM struct_with_missing as t
                """.trimIndent(),
                false,
                assertOnProblemCount(2, 0),
                BagType(closedStruct(StructType.Field("f1", StaticType.unionOf(StaticType.INT2, StaticType.MISSING))))
            ),
            TestCase(
                """
                    SELECT 
                        t.f1, -- OK
                        t.f2, -- always missing
                        t.f3 -- no such field
                    FROM struct_with_missing as t
                """.trimIndent(),
                true,
                assertOnProblemCount(0, 2),
                BagType(closedStruct(StructType.Field("f1", StaticType.unionOf(StaticType.INT2, StaticType.MISSING))))
            ),

            // TODO: EXCLUDE ERROR reporting is not completed.
            //  Currently we only handle root resolution.
            //  i.e., if the root of the exclude path is not resolved,
            //  we can report the problem.
            //  but we have not yet handled the situation in which
            //  the root is resolvable but the path is not.
            TestCase(
                """
                    SELECT * 
                        EXCLUDE t1.f1  -- no such root
                    FROM struct_no_missing as t
                """.trimIndent(),
                false,
                assertOnProblemCount(1, 0),
                BagType(closedStruct(StructType.Field("f1", StaticType.INT2)))
            ),
            TestCase(
                """
                    SELECT * 
                        EXCLUDE t1.f1  -- no such root
                    FROM struct_no_missing as t
                """.trimIndent(),
                true,
                assertOnProblemCount(0, 1),
                BagType(closedStruct(StructType.Field("f1", StaticType.INT2)))
            ),
//            TestCase(
//                """
//                    SELECT *
//                        EXCLUDE t.f2  -- no such field
//                    FROM struct_no_missing as t
//                """.trimIndent(),
//                false,
//                assertOnProblemCount(1, 0),
//                BagType(closedStruct(StructType.Field("f1", StaticType.INT2)))
//            ),
//            TestCase(
//                """
//                    SELECT *
//                        EXCLUDE t.f2  -- no such field
//                    FROM struct_no_missing as t
//                """.trimIndent(),
//                true,
//                assertOnProblemCount(0, 1),
//                BagType(closedStruct(StructType.Field("f1", StaticType.INT2)))
//            ),
        )
    }

    private fun runTestCase(tc: TestCase) {
        val planner = when (tc.isSignal) {
            true -> PartiQLPlanner.builder().signalMode().build()
            else -> PartiQLPlanner.builder().build()
        }
        val pc = ProblemCollector()
        val res = planner.plan(statement(tc.query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(
            plan, problems,
            *tc.assertion(problems).toTypedArray()
        )
        tc.expectedType.assertStaticTypeEqual((plan.statement as org.partiql.plan.Statement.Query).root.type)
    }

    @ParameterizedTest
    @MethodSource("testProblems")
    fun testProblems(tc: TestCase) = runTestCase(tc)

    @ParameterizedTest
    @MethodSource("testContinuation")
    fun testContinuation(tc: TestCase) = runTestCase(tc)

    private fun StaticType.assertStaticTypeEqual(other: StaticType) {
        val thisAll = this.allTypes.toSet()
        val otherAll = other.allTypes.toSet()
        val diff = (thisAll - otherAll) + (otherAll - thisAll)
        assert(diff.isEmpty()) {
            buildString {
                this.appendLine("expected: ")
                thisAll.forEach {
                    this.append("$it, ")
                }
                this.appendLine()
                this.appendLine("actual")
                otherAll.forEach {
                    this.append("$it, ")
                }
                this.appendLine()
                this.appendLine("diff")
                diff.forEach {
                    this.append("$it, ")
                }
            }
        }
    }
}
