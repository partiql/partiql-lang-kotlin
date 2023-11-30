/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.cli.shell

import org.partiql.cli.pipeline.AbstractPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.PartiQLResult
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A wrapper over [AbstractPipeline]. It constantly grabs input queries from [inputs] and places the [PartiQLResult]
 * in [results]. When it is done compiling a single statement, it sets [doneCompiling] to true.
 */
internal class RunnablePipeline(
    private val inputs: BlockingQueue<Input>,
    private val results: BlockingQueue<Output>,
    val pipeline: AbstractPipeline,
    private val doneCompiling: AtomicBoolean
) : Runnable {
    /**
     * When the Thread running this [Runnable] is interrupted, the underlying [AbstractPipeline] should catch the
     * interruption and fail with some exception. Then, we place the error (or result) in the output queue.
     */
    override fun run() {
        while (true) {
            val input = inputs.poll(3, TimeUnit.SECONDS)
            if (input != null) {
                try {
                    val result = pipeline.compile(input.input, input.session)
                    results.put(Output.Result(result))
                } catch (t: Throwable) {
                    results.put(Output.Error(t))
                }
                doneCompiling.set(true)
            }
        }
    }

    /**
     * Represents a PartiQL statement ([input]) and the [EvaluationSession] to evaluate with.
     */
    internal data class Input(
        val input: String,
        val session: EvaluationSession
    )

    internal sealed interface Output {
        data class Result(val result: PartiQLResult): Output
        data class Error(val throwable: Throwable): Output
    }
}
