package org.partiql.lang.eval

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.evaluatortestframework.CompilerPipelineFactory
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.eval.evaluatortestframework.PartiQLCompilerPipelineFactoryAsync
import org.partiql.lang.eval.evaluatortestframework.PipelineEvaluatorTestAdapter
import org.partiql.lang.mockdb.MockDb
import org.partiql.lang.util.testdsl.IonResultTestCase

class EvaluatorTests {
    private val mockDb = EVALUATOR_TEST_SUITE.mockDb()

    companion object {
        val AST_EVALUATOR_SKIP_LIST = hashSetOf(
            // https://github.com/partiql/partiql-lang-kotlin/issues/336
            "projectionIterationBehaviorUnfiltered_select_list",
            "projectionIterationBehaviorUnfiltered_select_star"
        )

        @JvmStatic
        @Suppress("UNUSED")
        fun astEvaluatorTests(): List<IonResultTestCase> {
            val unskippedTests = EVALUATOR_TEST_SUITE.getAllTests(AST_EVALUATOR_SKIP_LIST)

            return unskippedTests.map {
                it.copy(
                    note = "legacy typing",
                    compileOptionsBuilderBlock = {
                        it.compileOptionsBuilderBlock(this)
                        typingMode(TypingMode.LEGACY)
                    }
                )
            } +
                unskippedTests.map {
                    it.copy(
                        note = "permissive typing",
                        compileOptionsBuilderBlock = {
                            it.compileOptionsBuilderBlock(this)
                            typingMode(TypingMode.PERMISSIVE)
                        }
                    )
                }
        }

        private val PLAN_EVALUATOR_SKIP_LIST = hashSetOf(
            // below this line use features not supported by the current physical algebra compiler.
            // most fail due to not supporting foundational nodes like id, global_id and scan yet.
            // PartiQL's test cases are not all that cleanly separated.
            "topLevelCountDistinct", // TODO: Support aggregates in physical plans
            "topLevelCount", // TODO: Support aggregates in physical plans
            "topLevelAllCount", // TODO: Support aggregates in physical plans
            "topLevelSum", // TODO: Support aggregates in physical plans
            "topLevelAllSum", // TODO: Support aggregates in physical plans
            "topLevelDistinctSum", // TODO: Support aggregates in physical plans
            "topLevelMin", // TODO: Support aggregates in physical plans
            "topLevelDistinctMin", // TODO: Support aggregates in physical plans
            "topLevelAllMin", // TODO: Support aggregates in physical plans
            "topLevelMax", // TODO: Support aggregates in physical plans
            "topLevelDistinctMax", // TODO: Support aggregates in physical plans
            "topLevelAllMax", // TODO: Support aggregates in physical plans
            "topLevelAvg", // TODO: Support aggregates in physical plans
            "topLevelDistinctAvg", // TODO: Support aggregates in physical plans
            "topLevelAvgOnlyInt", // TODO: Support aggregates in physical plans
            "selectValueAggregate", // TODO: Support aggregates in physical plans
            "selectListCountStar", // TODO: Support aggregates in physical plans
            "selectListCountVariable", // TODO: Support aggregates in physical plans
            "selectListMultipleAggregates", // TODO: Support aggregates in physical plans
            "selectListMultipleAggregatesNestedQuery", // TODO: Support aggregates in physical plans
            "aggregateInSubqueryOfSelect", // TODO: Support aggregates in physical plans
            "aggregateInSubqueryOfSelectValue", // TODO: Support aggregates in physical plans
            "aggregateWithAliasingInSubqueryOfSelectValue", // TODO: Support aggregates in physical plans
            "selectDistinctWithAggregate", // TODO: Support aggregates in physical plans
            "selectDistinctAggregationWithGroupBy", // TODO: Support GROUP BY in physical plans
            "selectDistinctWithGroupBy", // TODO: Support GROUP BY in physical plans

            // UndefinedVariableBehavior.MISSING not supported by plan evaluator
            "undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing",
            "undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing",

            // we are currently not plumbed to be able to return the original letter casing of global variables.
            // (there are other tests in LogicalToLogicalResolvedVisitorTransform which cover this case for the
            // PlannerPipeline)
            "identifierCaseMismatch"
        )

        @JvmStatic
        @Suppress("UNUSED")
        fun planEvaluatorTests(): List<IonResultTestCase> =
            // Since the physical plan evaluator is a modified copy of the AST evaluator, it inherits the
            // AST evaluator's current skip list.  The physical plan evaluator also doesn't yet implement
            // everything that the AST evaluator does, so has a separate skip list.
            astEvaluatorTests().filter { it.name !in PLAN_EVALUATOR_SKIP_LIST }
    }

    @ParameterizedTest
    @MethodSource("astEvaluatorTests")
    fun astEvaluatorTests(tc: IonResultTestCase) = tc.runTestCase(mockDb, EvaluatorTestTarget.COMPILER_PIPELINE)

    @ParameterizedTest
    @MethodSource("planEvaluatorTests")
    fun planEvaluatorTests(tc: IonResultTestCase) = tc.runTestCase(mockDb, EvaluatorTestTarget.PARTIQL_PIPELINE)

    @ParameterizedTest
    @MethodSource("planEvaluatorTests")
    fun planEvaluatorTestsAsync(tc: IonResultTestCase) = tc.runTestCase(mockDb, EvaluatorTestTarget.PARTIQL_PIPELINE_ASYNC)
}

fun IonResultTestCase.runTestCase(
    db: MockDb,
    target: EvaluatorTestTarget,
    compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
) {

    val adapter = PipelineEvaluatorTestAdapter(
        when (target) {
            EvaluatorTestTarget.COMPILER_PIPELINE -> CompilerPipelineFactory()
            EvaluatorTestTarget.PARTIQL_PIPELINE -> PartiQLCompilerPipelineFactory()
            EvaluatorTestTarget.PARTIQL_PIPELINE_ASYNC -> PartiQLCompilerPipelineFactoryAsync()
            // We don't support ALL_PIPELINES here because each pipeline needs a separate skip list, which
            // is decided by the caller of this function.
            EvaluatorTestTarget.ALL_PIPELINES -> error("May only test one pipeline at a time with IonResultTestCase")
        }
    )

    val session = EvaluationSession.build {
        globals(db.valueBindings)
        parameters(EVALUATOR_TEST_SUITE.createParameters())
    }

    val tc = EvaluatorTestCase(
        groupName = "${this.group}:${this.name}",
        query = this.sqlUnderTest,
        expectedResult = this.expectedLegacyModeIonResult,
        expectedPermissiveModeResult = this.expectedPermissiveModeIonResult,
        expectedResultFormat = ExpectedResultFormat.ION,
        implicitPermissiveModeTest = false,
        compileOptionsBuilderBlock = this.compileOptionsBuilderBlock,
        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
        extraResultAssertions = extraAssertions
    )

    if (!this.expectFailure) {
        adapter.runEvaluatorTestCase(tc, session)
    } else {
        val message = "We expect test \"${this.name}\" to fail, but it did not. This check exists to ensure the " +
            "failing list is up to date."

        assertThrows<Throwable>(message) {
            adapter.runEvaluatorTestCase(tc, session)
        }
    }
}
