package org.partiql.planner

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Namespace
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.planner.util.ProblemCollector
import org.partiql.types.Field
import org.partiql.types.BagType
import org.partiql.types.PType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import kotlin.test.assertEquals

internal class PlannerErrorReportingTests {

    val catalogName = "mode_test"
    val userId = "test-user"
    val queryId = "query"

    val catalog = Catalog
        .builder()
        .name(catalogName)
        .createTable("missing_binding", PType.typeDynamic())
        .createTable("atomic", PType.typeSmallInt())
        .createTable("collection_no_missing_atomic", PType.typeBag(PType.typeSmallInt()))
        .createTable("collection_contain_missing_atomic",PType.typeBag(PType.typeSmallInt()))
        .createTable("struct_no_missing", PType.typeRow(listOf(Field.of("f1", PType.typeSmallInt()))))
        .createTable("struct_with_missing", PType.typeRow(listOf(Field.of("f1", PType.typeSmallInt()))))
        .build()

    val session = Session.builder()
        .namespace(Namespace.of(catalogName))
        .build()

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
        val expectedType: CompilerType
    ) {
        constructor(
            query: String,
            isSignal: Boolean,
            assertion: (List<Problem>) -> List<() -> Boolean>,
            expectedType: StaticType = StaticType.ANY
        ) : this(query, isSignal, assertion, PType.fromStaticType(expectedType).toCType())
    }

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
                assertOnProblemCount(0, 0),
                expectedType = PType.typeUnknown().toCType()
            ),
            TestCase(
                "MISSING",
                true,
                assertOnProblemCount(0, 0),
                expectedType = PType.typeUnknown().toCType()
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
                assertOnProblemCount(1, 0),
                expectedType = PType.typeInt().toCType()
            ),
            // This will be a non-resolved function error.
            // As plus does not contain a function that match argument type with
            // int32 and missing.
            //  Error in signaling mode.
            TestCase(
                "1 + MISSING",
                true,
                assertOnProblemCount(0, 1),
                expectedType = PType.typeInt().toCType()
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
            // TODO: We currently don't have a good way to retain missing value information. The following test
            //  could have 2 warnings. One for executing a path operation on a literal missing. And one for
            //  executing a path operation on an expression that is known to result in the missing value.
            TestCase(
                "MISSING['a'].a",
                false,
                assertOnProblemCount(1, 0)
            ),
            // TODO: We currently don't have a good way to retain missing value information. The following test
            //  could have 2 errors. One for executing a path operation on a literal missing. And one for
            //  executing a path operation on an expression that is known to result in the missing value.
            TestCase(
                "MISSING['a'].a",
                true,
                assertOnProblemCount(0, 1)
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
                StaticType.INT4
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
                StaticType.INT4
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
                StaticType.ANY
            ),
            TestCase(
                " 'a' + 'b' ",
                true,
                assertOnProblemCount(0, 1),
                StaticType.ANY
            ),

            // No function with given name is registered.
            // always going to return error regardless of mode.
            // The expected type for continuation is ANY.
            TestCase(
                "not_a_function(1)",
                false,
                assertOnProblemCount(0, 1),
                StaticType.ANY
            ),
            TestCase(
                "not_a_function(1)",
                true,
                assertOnProblemCount(0, 1),
                StaticType.ANY
            ),

            // 1 + not_a_function(1)
            //  The continuation will return all numeric type
            TestCase(
                "1 + not_a_function(1)",
                false,
                assertOnProblemCount(1, 1),
                StaticType.INT4,
            ),
            TestCase(
                "1 + not_a_function(1)",
                true,
                assertOnProblemCount(0, 2),
                StaticType.INT4,
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
                BagType(closedStruct(StructType.Field("f1", StaticType.INT2), StructType.Field("f2", StaticType.ANY)))
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
                BagType(closedStruct(StructType.Field("f1", StaticType.INT2), StructType.Field("f2", StaticType.ANY)))
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
                BagType(
                    closedStruct(
                        StructType.Field("f1", StaticType.INT2),
                        StructType.Field("f2", StaticType.ANY),
                        StructType.Field("f3", StaticType.ANY)
                    )
                )
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
                BagType(
                    closedStruct(
                        StructType.Field("f1", StaticType.INT2),
                        StructType.Field("f2", StaticType.ANY),
                        StructType.Field("f3", StaticType.ANY)
                    )
                )
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
            true -> PartiQLPlanner.builder().signal().build()
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
        assertEquals(tc.expectedType, (plan.statement as org.partiql.plan.Statement.Query).root.type)
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
