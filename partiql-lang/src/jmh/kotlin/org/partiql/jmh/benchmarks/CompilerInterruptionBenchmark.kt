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

package org.partiql.jmh.benchmarks

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
import org.partiql.jmh.utils.FORK_VALUE_RECOMMENDED
import org.partiql.jmh.utils.MEASUREMENT_ITERATION_VALUE_RECOMMENDED
import org.partiql.jmh.utils.MEASUREMENT_TIME_VALUE_RECOMMENDED
import org.partiql.jmh.utils.WARMUP_ITERATION_VALUE_RECOMMENDED
import org.partiql.jmh.utils.WARMUP_TIME_VALUE_RECOMMENDED
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.concurrent.TimeUnit

/**
 * These are the sample benchmarks to demonstrate how JMH benchmarks in PartiQL should be set up.
 * Refer this [JMH tutorial](http://tutorials.jenkov.com/java-performance/jmh.html) for more information on [Benchmark]s,
 * [BenchmarkMode]s, etc.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class CompilerInterruptionBenchmark {

    companion object {
        private const val FORK_VALUE: Int = FORK_VALUE_RECOMMENDED
        private const val MEASUREMENT_ITERATION_VALUE: Int = MEASUREMENT_ITERATION_VALUE_RECOMMENDED
        private const val MEASUREMENT_TIME_VALUE: Int = MEASUREMENT_TIME_VALUE_RECOMMENDED
        private const val WARMUP_ITERATION_VALUE: Int = WARMUP_ITERATION_VALUE_RECOMMENDED
        private const val WARMUP_TIME_VALUE: Int = WARMUP_TIME_VALUE_RECOMMENDED
    }

    @State(Scope.Thread)
    open class MyState {
        val parser = PartiQLParserBuilder.standard().build()
        val session = EvaluationSession.standard()
        val pipeline = CompilerPipeline.standard()
        val pipelineWithoutInterruption = CompilerPipeline.build {
            compileOptions(CompileOptions.standard().copy(interruptible = false))
        }

        val crossJoins = """
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
            ([1, 2, 3, 4]) as x12
        """.trimIndent()
        val crossJoinsAst = parser.parseAstStatement(crossJoins)

        val crossJoinsWithAggFunction = """
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
            ([1, 2, 3, 4]) as x11
        """.trimIndent()
        val crossJoinsAggAst = parser.parseAstStatement(crossJoinsWithAggFunction)

        val crossJoinsWithAggFunctionAndGroupBy = """
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
            ([1, 2, 3, 4]) as x11
            GROUP BY x1._1
        """.trimIndent()
        val crossJoinsAggGroupAst = parser.parseAstStatement(crossJoinsWithAggFunctionAndGroupBy)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun compileCrossJoinWithInterruptible(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.crossJoins)
        blackhole.consume(exprValue)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun compileCrossJoinWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipelineWithoutInterruption.compile(state.crossJoins)
        blackhole.consume(exprValue)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun compileCrossJoinAggFuncWithInterruptible(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.crossJoinsWithAggFunction)
        blackhole.consume(exprValue)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun compileCrossJoinAggFuncWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipelineWithoutInterruption.compile(state.crossJoinsWithAggFunction)
        blackhole.consume(exprValue)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun compileCrossJoinAggFuncGroupingWithInterruptible(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.crossJoinsWithAggFunctionAndGroupBy)
        blackhole.consume(exprValue)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun compileCrossJoinAggFuncGroupingWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipelineWithoutInterruption.compile(state.crossJoinsWithAggFunctionAndGroupBy)
        blackhole.consume(exprValue)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun evalCrossJoinWithInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipeline.compile(state.crossJoinsAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        blackhole.consume(value)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun evalCrossJoinWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipelineWithoutInterruption.compile(state.crossJoinsAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        blackhole.consume(value)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun evalCrossJoinAggWithInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipeline.compile(state.crossJoinsAggAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        blackhole.consume(value)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun evalCrossJoinAggWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipelineWithoutInterruption.compile(state.crossJoinsAggAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        blackhole.consume(value)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun evalCrossJoinAggGroupWithInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipeline.compile(state.crossJoinsAggGroupAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        blackhole.consume(value)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun evalCrossJoinAggGroupWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipelineWithoutInterruption.compile(state.crossJoinsAggGroupAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        blackhole.consume(value)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun iterCrossJoinWithInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipeline.compile(state.crossJoinsAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        value.forEach { blackhole.consume(it) }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun iterCrossJoinWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipelineWithoutInterruption.compile(state.crossJoinsAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        value.forEach { blackhole.consume(it) }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun iterCrossJoinAggWithInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipeline.compile(state.crossJoinsAggAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        value.forEach { blackhole.consume(it) }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun iterCrossJoinAggWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipelineWithoutInterruption.compile(state.crossJoinsAggAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        value.forEach { blackhole.consume(it) }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun iterCrossJoinAggGroupWithInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipeline.compile(state.crossJoinsAggGroupAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        value.forEach { blackhole.consume(it) }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun iterCrossJoinAggGroupWithoutInterruptible(state: MyState, blackhole: Blackhole) {
        val result = state.pipelineWithoutInterruption.compile(state.crossJoinsAggGroupAst).evaluate(state.session) as PartiQLResult.Value
        val value = result.value
        value.forEach { blackhole.consume(it) }
    }
}
