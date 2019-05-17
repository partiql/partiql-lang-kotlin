/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.testframework.testcar.benchmark

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.results.*
import org.openjdk.jmh.results.format.*
import org.openjdk.jmh.runner.*
import org.openjdk.jmh.runner.options.*
import java.io.*
import java.math.*

internal class JmhRunResultAdapter(private val inner: RunResult) : BenchmarkRunResult {
    private val stats = inner.primaryResult.getStatistics()

    override val label = inner.primaryResult.getLabel()!!
    override val mean = bigDecimal(stats.mean)
    override val standardDeviation = bigDecimal(stats.standardDeviation)
    override val n = stats.n
    override val min = bigDecimal(stats.min)
    override val max = bigDecimal(stats.max)
    override val unit = inner.primaryResult.getScoreUnit()!!
}

internal class JmhRunner : BenchmarkRunner {
    companion object {
        private val options = OptionsBuilder()
            .warmupIterations(15)
            .measurementIterations(20) // 2 minimum so it can calculate statistics
            .forks(1)
            .output("/dev/null") // we read the results and transform them into ion. The std JMH will just pollute stdout
            .mode(Mode.Throughput)
            .shouldFailOnError(true)
            .build()
    }

    override fun run(): BenchmarkRunResults {
        val jmhResults = Runner(options).run()

        val outStream = ByteArrayOutputStream()
        ResultFormatFactory.getInstance(ResultFormatType.JSON, PrintStream(outStream)).writeOut(jmhResults)

        val raw = outStream.toString(Charsets.UTF_8.name())
        val results = jmhResults.map { JmhRunResultAdapter(it) }

        return BenchmarkRunResults(raw, results)
    }
}

private fun bigDecimal(v: Double) = BigDecimal(v, MathContext.DECIMAL64)