package org.partiql.planner

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.Statement
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.planner.util.PErrorAlwaysMissingCollector
import org.partiql.planner.util.PErrorCollector
import org.partiql.planner.util.PlanPrinter
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.fromStaticType
import kotlin.test.assertEquals

internal class PlannerPErrorReportingTests {

    private val catalogName = "mode_test"

    private val catalog = Catalog
        .builder()
        .name(catalogName)
        .define(Table.empty("missing_binding", PType.dynamic()))
        .define(Table.empty("atomic", PType.smallint()))
        .define(Table.empty("collection_no_missing_atomic", PType.bag(PType.smallint())))
        .define(Table.empty("collection_contain_missing_atomic", PType.bag(PType.smallint())))
        .define(Table.empty("struct_no_missing", PType.row(listOf(PTypeField.of("f1", PType.smallint())))))
        .define(Table.empty("struct_with_missing", PType.row(listOf(PTypeField.of("f1", PType.smallint())))))
        .build()

    private val session = Session.builder()
        .catalog(catalogName)
        .catalogs(catalog)
        .build()

    private val parser = PartiQLParser.builder().build()

    private val statement: ((String) -> Statement) = { query ->
        val parseResult = parser.parse(query)
        assertEquals(1, parseResult.statements.size)
        parseResult.statements[0]
    }

    private fun assertProblem(
        plan: org.partiql.plan.Plan,
        collector: PErrorCollector,
        block: (PErrorCollector) -> Unit
    ) {
        try {
            block.invoke(collector)
        } catch (e: Throwable) {
            val str = buildString {
                this.appendLine("Assertion failed")

                this.appendLine("--------Plan---------")
                PlanPrinter.append(this, plan)

                this.appendLine("----------Problems---------")
                collector.problems.forEach {
                    this.appendLine(it.toString())
                }
            }
            throw AssertionError(str, e)
        }
    }

    data class TestCase(
        val query: String,
        val isSignal: Boolean,
        val assertion: (PErrorCollector) -> Unit,
        val expectedType: CompilerType
    ) {
        constructor(
            query: String,
            isSignal: Boolean,
            assertion: (PErrorCollector) -> Unit,
            expectedType: StaticType = StaticType.ANY
        ) : this(query, isSignal, assertion, fromStaticType(expectedType).toCType())
    }

    companion object {
        private fun closedStruct(vararg field: StructType.Field): StructType =
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

        private fun assertOnProblemCount(warningCount: Int, errorCount: Int): (PErrorCollector) -> Unit = { collector ->
            val actualErrorCount = collector.errors.size
            val actualWarningCount = collector.warnings.size
            val message = buildString {
                appendLine("Expected warnings : $warningCount")
                appendLine("Actual warnings   : $actualWarningCount")
                appendLine("Expected errors   : $errorCount")
                appendLine("Actual errors     : $actualErrorCount")
            }
            assertEquals(warningCount, actualWarningCount, message)
            assertEquals(errorCount, actualErrorCount, message)
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
                expectedType = PType.unknown().toCType()
            ),
            TestCase(
                "MISSING",
                true,
                assertOnProblemCount(0, 0),
                expectedType = PType.unknown().toCType()
            ),
            // Unresolved variable always signals (10.1.3)
            TestCase(
                "var_not_exist",
                true,
                assertOnProblemCount(0, 1),
                expectedType = PType.unknown().toCType()
            ),

            // Function propagates missing in quite mode
            TestCase(
                "1 + MISSING",
                false,
                assertOnProblemCount(1, 0),
                expectedType = PType.integer().toCType()
            ),
            // This will be a non-resolved function error.
            // As plus does not contain a function that match argument type with
            // int32 and missing.
            //  Error in signaling mode.
            TestCase(
                "1 + MISSING",
                true,
                assertOnProblemCount(0, 1),
                expectedType = PType.integer().toCType()
            ),
            // Attempting to do path navigation(symbol) on missing(which is not tuple)
            //  returns missing in quite mode, and error out in signal mode
            TestCase(
                "MISSING.a",
                false,
                assertOnProblemCount(1, 0),
            ),
            TestCase(
                "MISSING.a",
                true,
                assertOnProblemCount(0, 1),
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
                assertOnProblemCount(1, 0),
                expectedType = PType.unknown().toCType()
            ),
            TestCase(
                "MISSING['a']",
                true,
                assertOnProblemCount(0, 1),
                expectedType = PType.unknown().toCType()
            ),
            // Chained, demonstrate missing trace.
            // TODO: We currently don't have a good way to retain missing value information. The following test
            //  could have 2 warnings. One for executing a path operation on a literal missing. And one for
            //  executing a path operation on an expression that is known to result in the missing value.
            TestCase(
                "MISSING['a'].a",
                false,
                assertOnProblemCount(2, 0),
            ),
            // TODO: We currently don't have a good way to retain missing value information. The following test
            //  could have 2 errors. One for executing a path operation on a literal missing. And one for
            //  executing a path operation on an expression that is known to result in the missing value.
            TestCase(
                "MISSING['a'].a",
                true,
                assertOnProblemCount(0, 2),
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
                assertOnProblemCount(0, 1),
                StaticType.ANY,
            ),
            TestCase(
                "1 + not_a_function(1)",
                true,
                assertOnProblemCount(0, 1),
                StaticType.ANY,
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
                assertOnProblemCount(2, 0),
                BagType(closedStruct(StructType.Field("f1", StaticType.INT2)))
            ),
            TestCase(
                """
                    SELECT * 
                        EXCLUDE t1.f1  -- no such root
                    FROM struct_no_missing as t
                """.trimIndent(),
                true,
                assertOnProblemCount(1, 1),
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
        val planner = PartiQLPlanner.builder().signal(tc.isSignal).build()
        val pc = when (tc.isSignal) {
            true -> PErrorAlwaysMissingCollector()
            false -> PErrorCollector()
        }
        val pConfig = Context.of(pc)
        val res = planner.plan(statement(tc.query), session, pConfig)
        val plan = res.plan
        assertProblem(
            plan, pc,
            tc.assertion
        )
        val statement = plan.action as Action.Query
        val actualType = statement.rex.type.pType
        assertEquals(tc.expectedType, actualType)
    }

    @ParameterizedTest
    @MethodSource("testProblems")
    fun testProblems(tc: TestCase) = runTestCase(tc)

    @ParameterizedTest
    @MethodSource("testContinuation")
    fun testContinuation(tc: TestCase) = runTestCase(tc)
}
