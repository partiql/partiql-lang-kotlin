/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ION
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.testdsl.IonResultTestCase
import org.partiql.lang.util.testdsl.runTestCase

class EvaluatorTests {
    private val valueFactory = ExprValueFactory.standard(ION)
    private val mockDb = EVALUATOR_TEST_SUITE.mockDb(valueFactory)

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
            "selectCorrelatedUnpivot", // TODO: Support UNPIVOT in physical plans
            "nestedSelectJoinWithUnpivot", // TODO: Support UNPIVOT in physical plans
            "nestedSelectJoinLimit", // TODO: Support UNPIVOT in physical plans
            "pivotFrom", // TODO: Support PIVOT in physical plans
            "pivotLiteralFieldNameFrom", // TODO: Support PIVOT in physical plans
            "pivotBadFieldType", // TODO: Support PIVOT in physical plans
            "pivotUnpivotWithWhereLimit", // TODO: Support PIVOT in physical plans
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
            "unpivotStructWithMissingField", // TODO: Support UNPIVOT in physical plans
            "unpivotMissing", // TODO: Support UNPIVOT in physical plans
            "unpivotEmptyStruct", // TODO: Support UNPIVOT in physical plans
            "unpivotMissingWithAsAndAt", // TODO: Support UNPIVOT in physical plans
            "unpivotMissingCrossJoinWithAsAndAt", // TODO: Support UNPIVOT in physical plans

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
    fun astEvaluatorTests(tc: IonResultTestCase) = tc.runTestCase(valueFactory, mockDb, EvaluatorTestTarget.COMPILER_PIPELINE)

    @ParameterizedTest
    @MethodSource("planEvaluatorTests")
    fun planEvaluatorTests(tc: IonResultTestCase) = tc.runTestCase(valueFactory, mockDb, EvaluatorTestTarget.PLANNER_PIPELINE)
}
