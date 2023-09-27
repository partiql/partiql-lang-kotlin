package org.partiql.test.coverage.nested

import org.partiql.coverage.api.PartiQLTest
import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.PartiQLResult
import kotlin.test.assertEquals

/**
 * This is a simple test to check that nested packages can work.
 */
class SimpleTest {

    @PartiQLTest(provider = SuccessTestProvider.SuccessTestProvider::class)
    fun successTestExample(tc: SuccessTestProvider.SuccessTestProvider.ExampleTestCase, result: PartiQLResult.Value) {
        val value = result.value
        assertEquals(ExprValueType.BAG, value.type)
        assertEquals(tc.expectedSize, value.count())
    }

    object SuccessTestProvider {
        object SuccessTestProvider : PartiQLTestProvider {
            override val statement: String = """
                SELECT VALUE t
                FROM <<1, 2>> AS t
                WHERE x > 7 AND x < 15
            """

            override fun getPipelineBuilder(): CompilerPipeline.Builder? = null

            override fun getTestCases(): Iterable<ExampleTestCase> = listOf(
                ExampleTestCase(
                    session = EvaluationSession.build {
                        globals(
                            Bindings.ofMap(
                                mapOf(
                                    "x" to ExprValue.newInt(8)
                                )
                            )
                        )
                    },
                    expectedSize = 2
                ),
                ExampleTestCase(
                    session = EvaluationSession.build {
                        globals(
                            Bindings.ofMap(
                                mapOf(
                                    "x" to ExprValue.newInt(4)
                                )
                            )
                        )
                    },
                    expectedSize = 0
                ),
            )

            class ExampleTestCase(
                override val session: EvaluationSession,
                val expectedSize: Int
            ) : PartiQLTestCase
        }
    }
}
