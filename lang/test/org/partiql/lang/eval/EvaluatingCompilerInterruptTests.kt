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

import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.partiql.lang.CompilerPipeline
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Making sure we can interrupt the [EvaluatingCompiler].
 */
class EvaluatingCompilerInterruptTests {

    private val ion = IonSystemBuilder.standard().build()
    private val factory = ExprValueFactory.standard(ion)
    private val session = EvaluationSession.standard()
    private val pipeline = CompilerPipeline.standard(ion)

    companion object {
        /** How long (in millis) to wait after starting a thread to set the interrupted flag. */
        const val INTERRUPT_AFTER_MS: Long = 100

        /** How long (in millis) to wait for a thread to terminate after setting the interrupted flag. */
        const val WAIT_FOR_THREAD_TERMINATION_MS: Long = 1000
    }

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
        val expression = pipeline.compile(query)
        val result = expression.eval(session)
        testThreadInterrupt {
            result.forEach { it }
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
            [1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN
            ([1, 2, 3, 4] LEFT JOIN ([1, 2, 3, 4]) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE
        """.trimIndent()
        val expression = pipeline.compile(query)
        val result = expression.eval(session)
        testThreadInterrupt {
            result.forEach { it }
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
        val expression = pipeline.compile(query)
        testThreadInterrupt {
            expression.eval(session)
        }
    }

    /**
     * We need to make sure that we can end a never-ending query. These sorts of queries get materialized during the
     * iteration of [ExprValue].
     */
    @Test
    fun neverEndingScan() {
        val indefiniteCollection = factory.newBag(
            sequence {
                while (true) {
                    yield(factory.nullValue)
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

        val expression = pipeline.compile(query)
        val result = expression.eval(session)
        testThreadInterrupt {
            result.forEach { it }
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
