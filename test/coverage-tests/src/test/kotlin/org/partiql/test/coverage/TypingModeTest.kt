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
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.CoverageStructure
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.TypingMode
import org.partiql.test.coverage.utils.PartiQLTestCaseDefault
import kotlin.test.assertEquals

/**
 * Tests PERMISSIVE and LEGACY TypingModes
 */
class TypingModeTest {

    @PartiQLTest(provider = PermissiveModeTestProvider::class)
    fun testPermissiveConditions(tc: PartiQLTestCase, result: PartiQLResult.Value) {
        val data = result.getCoverageData()!!
        val structure = result.getCoverageStructure()!!

        // Assert Counts
        assertEquals(2, structure.branches.size)
        assertEquals(28, structure.branchConditions.size)

        // Assert we have the right number of TRUE, FALSE, NULL, MISSING counts
        val trueConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.TRUE
        }
        val falseConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.FALSE
        }
        val nullConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.NULL
        }
        val missingConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.MISSING
        }

        assertEquals(3, trueConditions.filter { (_, count) -> count > 0 }.size)
        assertEquals(0, falseConditions.filter { (_, count) -> count > 0 }.size)
        assertEquals(1, nullConditions.filter { (_, count) -> count > 0 }.size)
        assertEquals(3, missingConditions.filter { (_, count) -> count > 0 }.size)
    }

    @PartiQLTest(provider = LegacyModeTestProvider::class)
    fun testLegacyConditions(tc: PartiQLTestCase, result: PartiQLResult.Value) {
        val data = result.getCoverageData()!!
        val structure = result.getCoverageStructure()!!

        // Assert Counts
        assertEquals(2, structure.branches.size)
        assertEquals(21, structure.branchConditions.size)

        // Assert we have the right number of TRUE, FALSE, NULL, MISSING counts
        val trueConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.TRUE
        }
        val falseConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.FALSE
        }
        val nullConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.NULL
        }
        val missingConditions = data.branchConditionCount.filter { (key, _) ->
            structure.branchConditions[key]!!.outcome == CoverageStructure.BranchCondition.Outcome.MISSING
        }

        assertEquals(3, trueConditions.filter { (_, count) -> count > 0 }.size)
        assertEquals(0, falseConditions.filter { (_, count) -> count > 0 }.size)
        assertEquals(4, nullConditions.filter { (_, count) -> count > 0 }.size)
        assertEquals(0, missingConditions.size)
    }

    object PermissiveModeTestProvider : TypingModeTestProvider(TypingMode.PERMISSIVE)
    object LegacyModeTestProvider : TypingModeTestProvider(TypingMode.LEGACY)

    open class TypingModeTestProvider(private val mode: TypingMode) : PartiQLTestProvider {
        override val statement: String = """
            SELECT a
            FROM << { 'a': 1, 'b': NULL } >> AS t
            WHERE
                t.a > 0 -- PERMISSIVE: Should result in TRUE; LEGACY: TRUE
                AND -- PERMISSIVE: Should result in TRUE; LEGACY: TRUE
                t.a < 2 -- PERMISSIVE: Should result in TRUE; LEGACY: TRUE
                AND ( -- PERMISSIVE: Should result in MISSING; LEGACY: NULL
                    t.a > t.b -- PERMISSIVE: Should result in NULL; LEGACY: NULL
                    OR -- PERMISSIVE: Should result in MISSING; LEGACY: NULL
                    t.a < t.c -- PERMISSIVE: Should result in MISSING; LEGACY: NULL
                )
        """

        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(PartiQLTestCaseDefault())

        // Permissive Pipeline
        override fun getPipelineBuilder(): CompilerPipeline.Builder = CompilerPipeline.builder()
            .compileOptions(CompileOptions.Companion.build { typingMode(mode) })
    }
}
