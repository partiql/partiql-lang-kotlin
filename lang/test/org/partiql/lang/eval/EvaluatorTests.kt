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
import org.partiql.lang.util.testdsl.IonResultTestCase
import org.partiql.lang.util.testdsl.runTestCase

class EvaluatorTests {
    private val valueFactory = ExprValueFactory.standard(ION)
    private val mockDb = EVALUATOR_TEST_SUITE.mockDb(valueFactory)

    companion object {
        val SKIP_LIST = hashSetOf(
            // https://github.com/partiql/partiql-lang-kotlin/issues/169
            "selectDistinctStarLists", "selectDistinctStarBags", "selectDistinctStarMixed",

            // https://github.com/partiql/partiql-lang-kotlin/issues/336
            "projectionIterationBehaviorUnfiltered_select_list",
            "projectionIterationBehaviorUnfiltered_select_star",


            // below this line use features not supported by the current physical algebra compiler.
            // most fail due to not supporting foundational nodes like id, global_id and scan yet.
            // PartiQL's test cases are not all that cleanly separated.
            "selectCorrelatedUnpivot",  // TODO: PHYS_ALGEBRA_REFACTOR_UNPIVOT
            "nestedSelectJoinWithUnpivot", // TODO: PHYS_ALGEBRA_REFACTOR_UNPIVOT
            "nestedSelectJoinLimit", // TODO: PHYS_ALGEBRA_REFACTOR_UNPIVOT
            "pivotFrom", // TODO: PHYS_ALGEBRA_REFACTOR_PIVOT
            "pivotLiteralFieldNameFrom", // TODO: PHYS_ALGEBRA_REFACTOR_PIVOT
            "pivotBadFieldType", // TODO: PHYS_ALGEBRA_REFACTOR_PIVOT
            "pivotUnpivotWithWhereLimit", // TODO: PHYS_ALGEBRA_REFACTOR_PIVOT
            "topLevelCountDistinct", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelCount", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelAllCount", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelSum", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelAllSum", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelDistinctSum", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelMin", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelDistinctMin", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelAllMin", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelMax", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelDistinctMax", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelAllMax", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelAvg", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelDistinctAvg", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "topLevelAvgOnlyInt", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "selectValueAggregate", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "selectListCountStar", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "selectListCountVariable", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "selectListMultipleAggregates", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "selectListMultipleAggregatesNestedQuery", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "aggregateInSubqueryOfSelect", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "aggregateInSubqueryOfSelectValue", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "aggregateWithAliasingInSubqueryOfSelectValue", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "selectDistinctWithAggregate", // TODO: PHYS_ALGEBRA_REFACTOR_CALL_AGG
            "selectDistinctAggregationWithGroupBy",  // TODO: PHYS_ALGEBRA_REFACTOR_GROUP_BY
            "selectDistinctWithGroupBy", // TODO: PHYS_ALGEBRA_REFACTOR_GROUP_BY
            "unpivotMissing",  // TODO: PHYS_ALGEBRA_REFACTOR_UNPIVOT
            "unpivotEmptyStruct", // TODO: PHYS_ALGEBRA_REFACTOR_UNPIVOT
            "unpivotMissingWithAsAndAt", // TODO: PHYS_ALGEBRA_REFACTOR_UNPIVOT
            "unpivotMissingCrossJoinWithAsAndAt", // TODO: PHYS_ALGEBRA_REFACTOR_UNPIVOT
        )

        /** Test cases for `definedGlobalVariableTests`. */
        @JvmStatic
        @Suppress("UNUSED")
        fun definedVariableTestCases(): List<IonResultTestCase> {
            val unskippedTests = EVALUATOR_TEST_SUITE.getAllTests(SKIP_LIST)

            return unskippedTests.map {
                it.copy(
                    note = "legacy typing",
                    compileOptions = CompileOptions.build(it.compileOptions) { typingMode(TypingMode.LEGACY) }
                )
            } +
                unskippedTests.map {
                    it.copy(
                        note = "permissive typing",
                        compileOptions = CompileOptions.build(it.compileOptions) { typingMode(TypingMode.PERMISSIVE) }
                    )
                }
        }

        /** Test cases for `undefinedGlobalVariableTests`. */
        @JvmStatic
        @Suppress("UNUSED")
        fun undefinedVariableTestCases(): List<IonResultTestCase> {
            val testCasesWithVariablesThatShadowGlobal = setOf(
                "variableShadow",
                "selectNonCorrelatedJoin",
                "joinWithShadowedGlobal",
            )
            return definedVariableTestCases().map { tc ->
                tc.takeIf { it.name in testCasesWithVariablesThatShadowGlobal }?.copy(expectFailure = true) ?: tc
            }
        }
    }

    /**
     * Runs the tests with bindings that are known at compile-time.
     * In this scenario, global variables are unambiguously resolved at compile-time.
     */
    @ParameterizedTest
    @MethodSource("definedVariableTestCases")
    fun definedGlobalVariableTests(tc: IonResultTestCase) = tc.runTestCase(
        valueFactory,
        mockDb,
        defineGlobals = true
    )

    /**
     * Runs the tests with bindings that are not known at compile-time.
     *
     * In this scenario, global variables are dynamically resolved at evaluation time.  This should be identical
     * to [definedGlobalVariableTests], with only one exception: a local variable with the same name as a global
     * variable will always be resolved to the local variables, even in within a `(scan ...)` `<expr>`.
     */
    @ParameterizedTest
    @MethodSource("undefinedVariableTestCases")
    fun undefinedGlobalVariableTests(tc: IonResultTestCase) = tc.runTestCase(
        valueFactory,
        mockDb,
        defineGlobals = false
    )
}
