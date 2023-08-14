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
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.PartiQLResult
import kotlin.test.Ignore
import kotlin.test.assertEquals

class RunnerTest {

    @PartiQLTest(provider = SuccessTestProvider::class)
    fun successTestExample(tc: SuccessTestProvider.ExampleTestCase, result: PartiQLResult.Value) {
        val value = result.value
        assertEquals(ExprValueType.BAG, value.type)
        value.forEach { element ->
            assertEquals(ExprValueType.STRUCT, element.type)
        }
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
        override val statement: String = """
            SELECT *
            FROM << x, x, x, x, x, x, x, x, x, x >> AS x
            WHERE
            x > 0
            AND
            EXISTS (
                SELECT t AS t
                FROM << x, x, x >> AS t
                WHERE t > 7 AND t < 15
            )

        """

        override fun getTestCases(): Iterable<ExampleTestCase> = List(1001) {
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
            )
        }

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null

        class ExampleTestCase(
            val name: String,
            override val session: EvaluationSession,
            val expected: Boolean
        ) : PartiQLTestCase
    }

    object ComplexTestProvider : PartiQLTestProvider {
        override val statement: String = """
            SELECT
                t.a > 2 AS gtA,
                t.b < 3 AS ltB,
                CASE
                    WHEN (t.a > 2 OR t.a = 0)
                    THEN TRUE
                    WHEN (t.a + t.b = 1 AND t.a < 0)
                    THEN TRUE
                    ELSE FALSE
                END AS aLessThanTwo,
                CASE (t.a)
                    WHEN 0
                    THEN 'isZero'
                    WHEN 1
                    THEN 'is1' || CAST((t.a > t.b) AS STRING)
                    ELSE 'UNKNOWN'
                END AS textualRepresentation
            FROM (
                SELECT x.c = TRUE
                FROM <<
                    { 'a': globalA, 'b': globalB, 'c': globalA > globalB }
                >> AS x
                WHERE x.c = TRUE AND x.c = TRUE
            ) AS t
            WHERE
                (
                    SELECT t2.a = TRUE
                    FROM << { 'a': TRUE } >> AS t2
                    WHERE t2.a = TRUE
                )
                AND
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

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null
    }

    object InitializationFailureProvider : PartiQLTestProvider {
        override val statement: String = "x x x x x x"
        override fun getTestCases(): Iterable<PartiQLTestCase> = listOf(
            TestCase(EvaluationSession.standard())
        )

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null

        class TestCase(override val session: EvaluationSession) : PartiQLTestCase
    }

    object TestInitializationFailureProvider : PartiQLTestProvider {
        override val statement: String = "TRIM(x)"
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

        override fun getPipelineBuilder(): CompilerPipeline.Builder? = null

        class TestCase(override val session: EvaluationSession) : PartiQLTestCase
    }
}
