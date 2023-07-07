package org.partiql.test.coverage.simple

import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.coverage.api.PartiQLTestProvider
import org.partiql.coverage.api.params.PartiQLTest
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.booleanValue
import kotlin.test.assertEquals

class SimpleTest {

    @PartiQLTest(provider = SuccessTestProvider.SuccessTestProvider::class)
    fun successTestExample(tc: SuccessTestProvider.SuccessTestProvider.ExampleTestCase, result: ExprValue) {
        assertEquals(ExprValueType.BOOL, result.type)
        assertEquals(tc.expected, result.booleanValue())
    }

    object SuccessTestProvider {
        object SuccessTestProvider : PartiQLTestProvider {
            override val query: String = "x > 7 AND x < 15"

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
