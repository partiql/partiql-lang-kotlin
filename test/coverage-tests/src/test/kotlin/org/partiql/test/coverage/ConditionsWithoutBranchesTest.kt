/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.test.coverage

import org.partiql.coverage.api.PartiQLTest
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.PartiQLResult
import org.partiql.test.coverage.utils.PartiQLTestCaseDefault

/**
 * Tests to make sure conditions outside of branches don't get counted
 */
class ConditionsWithoutBranchesTest {

    @PartiQLTest(provider = ConditionsWithoutBranchesProvider::class)
    fun testConditionsWithoutBranchesDontProduceData(tc: PartiQLTestCase, result: PartiQLResult.Value) {
        val data = result.getCoverageData() ?: error("Coverage data should exist.")
        assert(data.branchCount.isEmpty())
        assert(data.branchConditionCount.isEmpty())
    }

    /**
     * Lots of boolean expressions outside of control-flow expressions.
     */
    object ConditionsWithoutBranchesProvider : PartiQLTestProvider {
        override val statement: String = """
            1 + 2 - 3 % 4 * 5 < 6
            AND 7 > 8 OR 8 = 9 AND 10 <> 11
            AND 12 IN [1, 2] OR 'hello' LIKE 'world' AND 1 BETWEEN 2 AND 3
        """.trimIndent()

        override fun getTestCases(): Iterable<PartiQLTestCase> = List(3) {
            PartiQLTestCaseDefault()
        }

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null
    }
}
