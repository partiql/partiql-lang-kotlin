/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.syntax.impl.INTERRUPT_AFTER_MS
import org.partiql.lang.syntax.impl.WAIT_FOR_THREAD_TERMINATION_MS
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Making sure we can interrupt the [EvaluatingCompiler].
 */
class EvaluatingCompilerInterruptTests {

    private val parser = PartiQLParserBuilder.standard().build()
    private val session = EvaluationSession.standard()
    private val options = CompileOptions.standard().copy(interruptible = true)
    private val compiler = EvaluatingCompiler(
        emptyList(),
        emptyMap(),
        emptyMap(),
        options
    )

    /**
     * Joins are only evaluated during the materialization of the ExprValue's elements. Cross Joins.
     */
    @Test
    fun evalCrossJoins() {
        val query = """
            SELECT
            *
            FROM
            ([1, 2, 3, 4]) as x1,
            ([1, 2, 3, 4]) as x2,
            ([1, 2, 3, 4]) as x3,
            ([1, 2, 3, 4]) as x4,
            ([1, 2, 3, 4]) as x5,
            ([1, 2, 3, 4]) as x6,
            ([1, 2, 3, 4]) as x7,
            ([1, 2, 3, 4]) as x8,
            ([1, 2, 3, 4]) as x9,
            ([1, 2, 3, 4]) as x10,
            ([1, 2, 3, 4]) as x11,
            ([1, 2, 3, 4]) as x12,
            ([1, 2, 3, 4]) as x13,
            ([1, 2, 3, 4]) as x14,
            ([1, 2, 3, 4]) as x15
        """.trimIndent()
        val ast = parser.parseAstStatement(query)
        val expression = compiler.compile(ast)
        val result = expression.evaluate(session) as PartiQLResult.Value
        testThreadInterrupt {
            result.value.forEach { it }
        }
    }

    /**
     * Joins are only evaluated during the materialization of the ExprValue's elements. Making sure left
     * joins can be interrupted.
     */
    @Test
    fun evalLeftJoins() {
        val query = """
            SELECT
            *
            FROM
            ([1, 2, 3, 4]) as x1 LEFT JOIN
            ([1, 2, 3, 4]) as x2 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x3 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x4 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x5 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x6 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x7 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x8 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x9 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x10 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x11 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x12 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x13 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x14 ON TRUE LEFT JOIN
            ([1, 2, 3, 4]) as x15 ON TRUE 
        """.trimIndent()
        val ast = parser.parseAstStatement(query)
        val expression = compiler.compile(ast)
        val result = expression.evaluate(session) as PartiQLResult.Value
        testThreadInterrupt {
            result.value.forEach { it }
        }
    }

    /**
     * Aggregations currently get materialized during [Expression.evaluate], so we need to check that we can
     * interrupt there.
     */
    @Test
    fun compileLargeAggregation() {
        val query = """
            SELECT
            COUNT(*)
            FROM
            ([1, 2, 3, 4]) as x1,
            ([1, 2, 3, 4]) as x2,
            ([1, 2, 3, 4]) as x3,
            ([1, 2, 3, 4]) as x4,
            ([1, 2, 3, 4]) as x5,
            ([1, 2, 3, 4]) as x6,
            ([1, 2, 3, 4]) as x7,
            ([1, 2, 3, 4]) as x8,
            ([1, 2, 3, 4]) as x9,
            ([1, 2, 3, 4]) as x10,
            ([1, 2, 3, 4]) as x11,
            ([1, 2, 3, 4]) as x12,
            ([1, 2, 3, 4]) as x13,
            ([1, 2, 3, 4]) as x14,
            ([1, 2, 3, 4]) as x15
        """.trimIndent()
        val ast = parser.parseAstStatement(query)
        val expression = compiler.compile(ast)
        testThreadInterrupt {
            expression.evaluate(session) as PartiQLResult.Value
        }
    }

    /**
     * We need to make sure that we can end a never-ending query. These sorts of queries get materialized during the
     * iteration of [ExprValue].
     */
    @Test
    fun neverEndingScan() {
        val indefiniteCollection = ExprValue.newBag(
            sequence {
                while (true) {
                    yield(ExprValue.nullValue)
                }
            }
        )
        val query = """
            SELECT *
            FROM ?
        """.trimIndent()
        val session = EvaluationSession.build {
            parameters(listOf(indefiniteCollection))
        }
        val ast = parser.parseAstStatement(query)
        val expression = compiler.compile(ast)
        val result = expression.evaluate(session) as PartiQLResult.Value
        testThreadInterrupt {
            result.value.forEach { it }
        }
    }

    private fun testThreadInterrupt(
        interruptAfter: Long = INTERRUPT_AFTER_MS,
        interruptWait: Long = WAIT_FOR_THREAD_TERMINATION_MS,
        block: () -> Unit
    ) {
        val wasInterrupted = AtomicBoolean(false)
        val t = thread(start = false) {
            try {
                block()
            } catch (_: InterruptedException) {
                wasInterrupted.set(true)
            } catch (e: EvaluationException) {
                if (e.cause is InterruptedException) {
                    wasInterrupted.set(true)
                }
            }
        }
        t.setUncaughtExceptionHandler { _, ex -> throw ex }
        t.start()
        Thread.sleep(interruptAfter)
        t.interrupt()
        t.join(interruptWait)
        Assertions.assertTrue(wasInterrupted.get(), "Thread should have been interrupted.")
    }
}
