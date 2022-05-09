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
        val SKIP_LIST = hashSetOf(
            // https://github.com/partiql/partiql-lang-kotlin/issues/169
            "selectDistinctStarLists", "selectDistinctStarBags", "selectDistinctStarMixed",

            // https://github.com/partiql/partiql-lang-kotlin/issues/336
            "projectionIterationBehaviorUnfiltered_select_list",
            "projectionIterationBehaviorUnfiltered_select_star"
        )

        @JvmStatic
        @Suppress("UNUSED")
        fun evaluatorTests(): List<IonResultTestCase> {
            val unskippedTests = EVALUATOR_TEST_SUITE.getAllTests(SKIP_LIST)

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
    }

    @ParameterizedTest
    @MethodSource("evaluatorTests")
    fun allTests(tc: IonResultTestCase) = tc.runTestCase(valueFactory, mockDb, EvaluatorTestTarget.COMPILER_PIPELINE)
}
