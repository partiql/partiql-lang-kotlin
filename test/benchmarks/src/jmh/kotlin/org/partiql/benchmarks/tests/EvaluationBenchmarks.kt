/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.benchmarks.tests

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import org.partiql.benchmarks.compiler.CompilerBuilder
import java.util.concurrent.TimeUnit

/**
 * These are benchmarks aimed at testing the performance of the [org.partiql.eval.value.Datum.comparator]. It is
 * internally used by aggregations (GROUP BY), distinct (DISTINCT), and the comparison operators (<, >, etc.).
 *
 * Here are the results for the commit attached to this Git blame:
 *
 * Benchmark                                             Mode  Cnt     Score    Error  Units
 * ComparatorBenchmarks.testStatement1EvalNew            avgt   20   183.489 ±  6.037  us/op
 * ComparatorBenchmarks.testStatement1EvalOld            avgt   20   193.418 ±  3.195  us/op
 * ComparatorBenchmarks.testStatement1Legacy             avgt   20     3.687 ±  0.031  us/op
 * ComparatorBenchmarks.testStatement1LegacyCompilation  avgt   20  1023.316 ± 22.940  us/op
 * ComparatorBenchmarks.testStatement2EvalNew            avgt   20    56.268 ±  1.290  us/op
 * ComparatorBenchmarks.testStatement2EvalOld            avgt   20   117.920 ±  2.821  us/op
 * ComparatorBenchmarks.testStatement2Legacy             avgt   20     0.870 ±  0.011  us/op
 * ComparatorBenchmarks.testStatement2LegacyCompilation  avgt   20   688.143 ±  6.328  us/op
 * ComparatorBenchmarks.testStatement3EvalNew            avgt   20    60.524 ±  0.488  us/op
 * ComparatorBenchmarks.testStatement3EvalOld            avgt   20   149.095 ±  0.599  us/op
 * ComparatorBenchmarks.testStatement3Legacy             avgt   20   327.299 ±  2.408  us/op
 * ComparatorBenchmarks.testStatement3LegacyCompilation  avgt   20   441.011 ±  3.382  us/op
 *
 * As you can see above, the EVAL_NEW engine performs significantly better than the V1 EVAL engine from V1.0.0-perf.1
 * (EVAL_OLD). On top of that, the EVAL_NEW engine also performed significantly better than the LEGACY evaluator. The
 * numbers presented by LEGACY are a bit more difficult to interpret because there is currently a bug in the LEGACY
 * evaluator that actually materializes aggregations *during* compilation (not at evaluation). Therefore, I've also
 * included the compilation time for the legacy engine.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Suppress("UNUSED")
open class ComparatorBenchmarks {

    companion object {
        private const val FORK_VALUE: Int = FORK_VALUE_RECOMMENDED
        private const val MEASUREMENT_ITERATION_VALUE: Int = MEASUREMENT_ITERATION_VALUE_RECOMMENDED
        private const val MEASUREMENT_TIME_VALUE: Int = MEASUREMENT_TIME_VALUE_RECOMMENDED
        private const val WARMUP_ITERATION_VALUE: Int = WARMUP_ITERATION_VALUE_RECOMMENDED
        private const val WARMUP_TIME_VALUE: Int = WARMUP_TIME_VALUE_RECOMMENDED
    }

    @State(Scope.Thread)
    open class MyState {

        private val compilerEvalCurrent = CompilerBuilder().current().build()
        private val compilerEvalOld = CompilerBuilder().version(CompilerBuilder.Version.EVAL_V1_0_0_PERF_1).build()
        val compilerLegacy = CompilerBuilder().version(CompilerBuilder.Version.LEGACY_V1_0_0_PERF_1).build()

        // #1
        val statement1EvalCurrent = compilerEvalCurrent.compile(Statements.STATEMENT_1)
        val statement1EvalOld = compilerEvalOld.compile(Statements.STATEMENT_1)
        val statement1Legacy = compilerLegacy.compile(Statements.STATEMENT_1)

        // #2
        val statement2EvalCurrent = compilerEvalCurrent.compile(Statements.STATEMENT_2)
        val statement2EvalOld = compilerEvalOld.compile(Statements.STATEMENT_2)
        val statement2Legacy = compilerLegacy.compile(Statements.STATEMENT_2)

        // #3
        val statement3EvalCurrent = compilerEvalCurrent.compile(Statements.STATEMENT_3)
        val statement3EvalOld = compilerEvalOld.compile(Statements.STATEMENT_3)
        val statement3Legacy = compilerLegacy.compile(Statements.STATEMENT_3)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement1EvalNew(state: MyState, blackhole: Blackhole) {
        val collection = state.statement1EvalCurrent
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement1EvalOld(state: MyState, blackhole: Blackhole) {
        val collection = state.statement1EvalOld
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement1Legacy(state: MyState, blackhole: Blackhole) {
        val collection = state.statement1Legacy
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement1LegacyCompilation(state: MyState, blackhole: Blackhole) {
        val collection = state.compilerLegacy.compile(Statements.STATEMENT_1)
        blackhole.consume(collection)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement2EvalNew(state: MyState, blackhole: Blackhole) {
        val collection = state.statement2EvalCurrent
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement2EvalOld(state: MyState, blackhole: Blackhole) {
        val collection = state.statement2EvalOld
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement2Legacy(state: MyState, blackhole: Blackhole) {
        val collection = state.statement2Legacy
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement2LegacyCompilation(state: MyState, blackhole: Blackhole) {
        val collection = state.compilerLegacy.compile(Statements.STATEMENT_2)
        blackhole.consume(collection)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement3EvalNew(state: MyState, blackhole: Blackhole) {
        val collection = state.statement3EvalCurrent
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement3EvalOld(state: MyState, blackhole: Blackhole) {
        val collection = state.statement3EvalOld
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement3Legacy(state: MyState, blackhole: Blackhole) {
        val collection = state.statement3Legacy
        collection.forEach {
            blackhole.consume(it)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testStatement3LegacyCompilation(state: MyState, blackhole: Blackhole) {
        val collection = state.compilerLegacy.compile(Statements.STATEMENT_3)
        blackhole.consume(collection)
    }
}
