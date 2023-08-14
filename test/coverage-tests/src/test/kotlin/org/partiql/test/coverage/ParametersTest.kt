package org.partiql.test.coverage

import org.partiql.coverage.api.PartiQLTest
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CoverageStructure
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.numberValue
import kotlin.test.assertEquals

class ParametersTest {
    @PartiQLTest(provider = ParameterizedTestProvider::class)
    fun testParameters(tc: ParameterizedTestProvider.ParameterizedTestCase, result: PartiQLResult.Value) {
        // Assert Coverage Data
        val structure = result.getCoverageStructure()!!
        val data = result.getCoverageData()!!
        val expectedBranchId = structure.branches.filter { (_, branch) -> branch.outcome == tc.expectedBranchOutcome }.keys.first()
        val unexpectedBranchIds = structure.branches.filter { (_, branch) -> branch.outcome != tc.expectedBranchOutcome }.keys
        assert(data.branchCount[expectedBranchId] != 0L)
        unexpectedBranchIds.forEach { branchId ->
            assertEquals(0, data.branchCount[branchId] ?: 0)
        }

        // Assert on Values
        assertEquals(ExprValueType.BAG, result.value.type)
        assertEquals(tc.expectedSize, result.value.count())
        tc.expectedResult?.let { expectedResult ->
            assertEquals(expectedResult, result.value.first().numberValue().toInt())
        }
    }

    object ParameterizedTestProvider : PartiQLTestProvider {
        override val statement: String = """
            SELECT VALUE a + ?
            FROM << { 'a': 100 } >> AS t
            WHERE t.a > ?
        """.trimIndent()

        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(
            ParameterizedTestCase(ExprValue.newInt(1), ExprValue.newInt(50), 1, 101, CoverageStructure.Branch.Outcome.TRUE),
            ParameterizedTestCase(ExprValue.newInt(2), ExprValue.newInt(99), 1, 102, CoverageStructure.Branch.Outcome.TRUE),
            ParameterizedTestCase(ExprValue.newInt(3), ExprValue.newInt(101), 0, null, CoverageStructure.Branch.Outcome.FALSE)
        )

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null

        class ParameterizedTestCase(
            private val first: ExprValue,
            private val second: ExprValue,
            val expectedSize: Int,
            val expectedResult: Int?,
            val expectedBranchOutcome: CoverageStructure.Branch.Outcome
        ) : PartiQLTestCase {
            override val session: EvaluationSession = EvaluationSession.build {
                parameters(listOf(first, second))
            }
        }
    }
}
