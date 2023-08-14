package org.partiql.test.coverage

import org.partiql.coverage.api.PartiQLTest
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.CoverageStructure
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.stringValue
import kotlin.test.assertEquals

class CaseWhenTests {

    @PartiQLTest(provider = SimpleCaseTestProvider::class)
    fun simpleCaseTests(tc: SimpleCaseTestCase, result: PartiQLResult.Value) {
        // Coverage Structure Assertions
        val structure = result.getCoverageStructure()!!
        assertEquals(6, structure.branches.size)

        // Coverage Data Assertions
        // We're just checking that the right line evaluated to TRUE
        val data = result.getCoverageData()!!
        if (tc.trueLineNumber != null) {
            val trues = structure.branches.filter { it.value.outcome == CoverageStructure.Branch.Outcome.TRUE }
            val hits = data.branchCount.filter { (_, count) -> count > 0 }.filter { trues.containsKey(it.key) }
            assertEquals(1, hits.size)
            val hit = hits.firstNotNullOf { it.key }
            val line = trues[hit]!!.line
            assertEquals(tc.trueLineNumber, line)
        }

        // Assert Result
        assertEquals(tc.expected, result.value.stringValue())
    }

    @PartiQLTest(provider = SearchedCaseTestProvider::class)
    fun searchedCaseTests(tc: SimpleCaseTestCase, result: PartiQLResult.Value) {
        // Coverage Structure Assertions
        val structure = result.getCoverageStructure()!!
        assertEquals(6, structure.branches.size)
        assertEquals(9, structure.branchConditions.size)

        // Branch Coverage Data Assertions
        // We're just checking that the right line evaluated to TRUE
        val data = result.getCoverageData()!!
        if (tc.trueLineNumber != null) {
            val trues = structure.branches.filter { it.value.outcome == CoverageStructure.Branch.Outcome.TRUE }
            val hits = data.branchCount.filter { (_, count) -> count > 0 }.filter { trues.containsKey(it.key) }
            assertEquals(1, hits.size)
            val hit = hits.firstNotNullOf { it.key }
            val line = trues[hit]!!.line
            assertEquals(tc.trueLineNumber, line)
        }

        // Branch Condition Coverage Data Assertions
        // We're just checking that the right line evaluated to TRUE
        if (tc.trueLineNumber != null) {
            val trues = structure.branchConditions.filter { it.value.outcome == CoverageStructure.BranchCondition.Outcome.TRUE }
            val hits = data.branchConditionCount.filter { (_, count) -> count > 0 }.filter { trues.containsKey(it.key) }
            assertEquals(1, hits.size)
            val hit = hits.firstNotNullOf { it.key }
            val line = trues[hit]!!.line
            assertEquals(tc.trueLineNumber, line)
        }

        // Assert Result
        assertEquals(tc.expected, result.value.stringValue())
    }

    object SearchedCaseTestProvider : PartiQLTestProvider {
        override val statement: String = """
            CASE
            WHEN x = 1 THEN '1'
            WHEN x = 2 THEN '2'
            WHEN x = 3 THEN '3'
            ELSE '4'
            END
        """.trimIndent()

        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(
            SimpleCaseTestCase(1, "1", 2),
            SimpleCaseTestCase(2, "2", 3),
            SimpleCaseTestCase(3, "3", 4),
            SimpleCaseTestCase(4, "4", null),
            SimpleCaseTestCase(100, "4", null),
        )

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null
    }

    object SimpleCaseTestProvider : PartiQLTestProvider {
        override val statement: String = """
            CASE (x)
            WHEN 1 THEN '1'
            WHEN 2 THEN '2'
            WHEN 3 THEN '3'
            ELSE '4'
            END
        """.trimIndent()

        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(
            SimpleCaseTestCase(1, "1", 2),
            SimpleCaseTestCase(2, "2", 3),
            SimpleCaseTestCase(3, "3", 4),
            SimpleCaseTestCase(4, "4", null),
            SimpleCaseTestCase(100, "4", null),
        )

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null
    }

    class SimpleCaseTestCase(private val x: Int, val expected: String, val trueLineNumber: Long?) : PartiQLTestCase {
        override val session: EvaluationSession = EvaluationSession.build {
            globals(
                Bindings.ofMap(
                    mapOf(
                        "x" to ExprValue.newInt(x)
                    )
                )
            )
        }
    }
}
