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
import org.partiql.lang.eval.booleanValue
import kotlin.test.assertEquals

/**
 * This is a simple test to check that nested packages can work.
 */
class SimpleTest {

    @PartiQLTest(provider = SuccessTestProvider.SuccessTestProvider::class)
    fun successTestExample(tc: SuccessTestProvider.SuccessTestProvider.ExampleTestCase, result: PartiQLResult.Value) {
        val value = result.value
        assertEquals(ExprValueType.BOOL, value.type)
        assertEquals(tc.expected, value.booleanValue())
    }

    object SuccessTestProvider {
        object SuccessTestProvider : PartiQLTestProvider {
            override val statement: String = "x > 7 AND x < 15"

            override fun getPipelineBuilder(): CompilerPipeline.Builder? = null

            override fun getTestCases(): Iterable<ExampleTestCase> = listOf(
                ExampleTestCase(
                    name = "Test #1",
                    session = EvaluationSession.build {
                        globals(
                            Bindings.ofMap(
                                mapOf(
                                    "x" to ExprValue.newInt(8)
                                )
                            )
                        )
                    },
                    expected = true
                ),
                ExampleTestCase(
                    name = "Test #2",
                    session = EvaluationSession.build {
                        globals(
                            Bindings.ofMap(
                                mapOf(
                                    "x" to ExprValue.newInt(4)
                                )
                            )
                        )
                    },
                    expected = false
                ),
            )

            class ExampleTestCase(
                val name: String,
                override val session: EvaluationSession,
                val expected: Boolean
            ) : PartiQLTestCase
        }
    }
}
