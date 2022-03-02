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
            "undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing",
            "selectFromListAndAtUnpivotWildCardOverScalar",
            "selectFromBagAndAtUnpivotWildCardOverScalar",
            "selectPathUnpivotWildCardOverStructMultiple",
            "ordinalAccessWithNegativeIndex",
            "ordinalAccessWithNegativeIndexAndBindings",
            "rangeTwiceOverScalar",
            "rangeOverNestedWithAt",
            "explicitAliasSelectSingleSource",
            "selectImplicitAndExplicitAliasSingleSourceHoisted",
            "syntheticColumnNameInSelect",
            "properAliasFromPathInSelect",
            "selectListWithMissing",
            "selectCrossProduct",
            "selectJoin",
            "selectCorrelatedJoin",
            "selectCorrelatedLeftJoin",
            "selectCorrelatedLeftJoinOnClause",
            "selectJoinOnClauseScoping",
            "selectNonCorrelatedJoin",
            "selectCorrelatedUnpivot",
            "nestedSelectJoinWithUnpivot",
            "nestedSelectJoinLimit",
            "correlatedJoinWithShadowedAttributes",
            "correlatedJoinWithoutLexicalScope",
            "joinWithShadowedGlobal",
            "pivotFrom",
            "pivotLiteralFieldNameFrom",
            "pivotBadFieldType",
            "pivotUnpivotWithWhereLimit",
            "topLevelCountDistinct",
            "topLevelCount",
            "topLevelAllCount",
            "topLevelSum",
            "topLevelAllSum",
            "topLevelDistinctSum",
            "topLevelMin",
            "topLevelDistinctMin",
            "topLevelAllMin",
            "topLevelMax",
            "topLevelDistinctMax",
            "topLevelAllMax",
            "topLevelAvg",
            "topLevelDistinctAvg",
            "topLevelAvgOnlyInt",
            "selectValueAggregate",
            "selectListCountStar",
            "selectListCountVariable",
            "selectListMultipleAggregates",
            "selectListMultipleAggregatesNestedQuery",
            "aggregateInSubqueryOfSelect",
            "aggregateInSubqueryOfSelectValue",
            "aggregateWithAliasingInSubqueryOfSelectValue",
            "undefinedUnqualifiedVariable_inSelect_withProjectionOption",
            "wildcardOrderedNames",
            "aliasWildcardOrderedNames",
            "aliasWildcardOrderedNamesSelectList",
            "aliasOrderedNamesSelectList",
            "selectDistinct",
            "selectDistinctWithAggregate",
            "selectDistinctSubQuery",
            "selectDistinctWithSubQuery",
            "selectDistinctAggregationWithGroupBy",
            "selectDistinctWithGroupBy",
            "selectDistinctWithJoin",
            "selectDistinctStarScalars",
            "selectDistinctStarStructs",
            "selectDistinctStarUnknowns",
            "selectDistinctStarIntegers",
            "selectDistinctValue",
            "selectDistinctExpressionAndWhere",
            "selectDistinctExpression",
            "projectOfListOfList",
            "projectOfBagOfBag",
            "projectOfListOfBag",
            "projectOfBagOfList",
            "projectOfSexp",
            "projectOfUnpivotPath",
            "parameters",
            "unpivotMissing",
            "unpivotEmptyStruct",
            "unpivotMissingWithAsAndAt",
            "unpivotMissingCrossJoinWithAsAndAt",
            "variableShadow",
            "selectIndexStruct",
            "implicitAliasSelectSingleSource",
            "selectValues",
            "explicitAliasSelectSingleSourceWithWhere",
            "undefinedQualifiedVariableWithUndefinedVariableBehaviorError",
            "emptySymbol",
            "emptySymbolInGlobals",
            "semicolonAtEndOfExpression",
            "dateTimePartsAsVariableNames",
        )

        // DL TODO: need to duplicate these tests--some also should run without a working [GlobalBindings]
        @JvmStatic
        @Suppress("UNUSED")
        fun evaluatorTests(): List<IonResultTestCase> {
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
    }

    @ParameterizedTest
    @MethodSource("evaluatorTests")
    fun allTests(tc: IonResultTestCase) = tc.runTestCase(valueFactory, mockDb)
}
