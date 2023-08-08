package org.partiql.test.coverage

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
import kotlin.test.Ignore
import kotlin.test.assertEquals

class RunnerTest {

    @PartiQLTest(provider = SuccessTestProvider::class)
    fun successTestExample(tc: SuccessTestProvider.ExampleTestCase, result: PartiQLResult.Value) {
        val value = result.value
        assertEquals(ExprValueType.BOOL, value.type)
        assertEquals(tc.expected, value.booleanValue())
    }

    @PartiQLTest(provider = ComplexTestProvider::class)
    fun complexTestExample(tc: SimpleTestCase, result: PartiQLResult.Value) {
        val value = result.value
        assertEquals(ExprValueType.BAG, value.type)
        value.forEach { element ->
            assertEquals(ExprValueType.STRUCT, element.type)
        }
    }

    @Ignore
    @PartiQLTest(provider = InitializationFailureProvider::class)
    fun initializationFailureExample(tc: InitializationFailureProvider.TestCase, result: ExprValue) {
        assertEquals(ExprValueType.BOOL, result.type)
    }

    @Ignore
    @PartiQLTest(provider = TestInitializationFailureProvider::class)
    fun testInitializationFailureExample(tc: TestInitializationFailureProvider.TestCase, result: ExprValue) {
        assertEquals(ExprValueType.STRING, result.type)
    }

    class SimpleTestCase(
        val name: String,
        private val globals: Map<String, String> = emptyMap(),
        private val parameters: List<String> = emptyList()
    ) : PartiQLTestCase {
        override val session: EvaluationSession = EvaluationSession.build {
            val globalsPipeline = CompilerPipeline.standard()
            val globalValues = globals.entries.associate { (globalName, valueAsPartiQLString) ->
                val emptySession = EvaluationSession.standard()
                val exprValue = globalsPipeline.compile(valueAsPartiQLString).eval(emptySession)
                globalName to exprValue
            }
            val parameterValues = parameters.map { parameterAsString ->
                val emptySession = EvaluationSession.standard()
                globalsPipeline.compile(parameterAsString).eval(emptySession)
            }
            globals(Bindings.ofMap(globalValues))
            parameters(parameterValues)
        }
    }

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

    object ComplexTestProvider : PartiQLTestProvider {
        override val query: String = """
            SELECT
                t.a AS a,
                t.b AS b,
                CASE
                    WHEN (t.a > 2 OR t.a = 0)
                    THEN TRUE
                    WHEN (t.a + t.b = 1 AND t.a < 0)
                    THEN TRUE
                    ELSE FALSE
                END AS aLessThanTwo
            FROM <<
                { 'a': globalA, 'b': globalB }
            >> AS t
            WHERE
                t.b < 3 AND t.b > 0
        """.trimIndent()

        override fun getTestCases(): Iterable<SimpleTestCase> = listOf(
            SimpleTestCase(
                name = "Simple",
                globals = mapOf(
                    "globalA" to "0",
                    "globalB" to "1"
                )
            ),
            SimpleTestCase(
                name = "Simple1",
                globals = mapOf(
                    "globalA" to "3",
                    "globalB" to "1"
                )
            ),
        )
    }

    object InitializationFailureProvider : PartiQLTestProvider {
        override val query: String = "x x x x x x"
        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(
            TestCase(EvaluationSession.standard())
        )

        class TestCase(override val session: EvaluationSession) : PartiQLTestCase
    }

    object TestInitializationFailureProvider : PartiQLTestProvider {
        override val query: String = "TRIM(x)"
        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(
            TestCase(
                session = EvaluationSession.build {
                    globals(
                        Bindings.ofMap(
                            mapOf(
                                "x" to ExprValue.newString(" hello ")
                            )
                        )
                    )
                }
            ),
            TestCase(
                session = EvaluationSession.build {
                    globals(
                        Bindings.ofMap(
                            mapOf(
                                "x" to ExprValue.newInt(5)
                            )
                        )
                    )
                }
            )
        )

        class TestCase(override val session: EvaluationSession) : PartiQLTestCase
    }
}
