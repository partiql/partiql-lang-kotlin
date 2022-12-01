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

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
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
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.concurrent.TimeUnit

// TODO: If https://github.com/benchmark-action/github-action-benchmark/issues/141 gets fixed, we can move to using
//  parameterized tests.

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
internal open class ParserBenchmark {

    companion object {
        private const val FORK_VALUE: Int = FORK_VALUE_RECOMMENDED
        private const val MEASUREMENT_ITERATION_VALUE: Int = MEASUREMENT_ITERATION_VALUE_RECOMMENDED
        private const val MEASUREMENT_TIME_VALUE: Int = MEASUREMENT_TIME_VALUE_RECOMMENDED
        private const val WARMUP_ITERATION_VALUE: Int = WARMUP_ITERATION_VALUE_RECOMMENDED
        private const val WARMUP_TIME_VALUE: Int = WARMUP_TIME_VALUE_RECOMMENDED
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex0(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[0])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex1(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[1])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex2(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[2])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex3(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[3])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex4(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[4])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex5(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[5])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex6(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[6])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex7(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[7])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex8(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[8])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex9(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[9])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex10(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[10])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex11(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[11])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex12(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[12])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex13(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[13])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex14(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[14])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex15(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[15])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex16(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[16])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex17(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[17])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex18(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[18])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex19(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[19])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex20(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[20])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex21(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[21])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex22(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[22])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex23(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[23])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex24(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[24])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex25(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[25])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex26(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[26])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex27(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[27])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex28(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[28])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex29(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[29])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex30(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[30])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex31(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[31])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSuccessQueryIndex32(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.queries[32])
        blackhole.consume(expr)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex0(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[0])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex1(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[1])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex2(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[2])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex3(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[3])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex4(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[4])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex5(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[5])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex6(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[6])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex7(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[7])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex8(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[8])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex9(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[9])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex10(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[10])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex11(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[11])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex12(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[12])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex13(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[13])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex14(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[14])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex15(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[15])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex16(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[16])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex17(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[17])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex18(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[18])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex19(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[19])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex20(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[20])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex21(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[21])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex22(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[22])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex23(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[23])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex24(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[24])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex25(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[25])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex26(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[26])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex27(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[27])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex28(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[28])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex29(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[29])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex30(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[30])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex31(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[31])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFailQueryIndex32(state: MyState, blackhole: Blackhole) {
        try {
            val expr = state.parser.parseAstStatement(state.queriesFailingHalfway[32])
            blackhole.consume(expr)
            throw RuntimeException()
        } catch (ex: ParserException) {
            blackhole.consume(ex)
        }
    }

    @State(Scope.Thread)
    open class MyState {

        private val ion: IonSystem = IonSystemBuilder.standard().build()
        val parser = PartiQLParserBuilder().ionSystem(ion).build()

        private val query15OrsAndLikes = """
            SELECT * 
            FROM hr.employees as emp
            WHERE lower(emp.name) LIKE '%bob smith%'
               OR lower(emp.name) LIKE '%gage swanson%'
               OR lower(emp.name) LIKE '%riley perry%'
               OR lower(emp.name) LIKE '%sandra woodward%'
               OR lower(emp.name) LIKE '%abagail oconnell%'
               OR lower(emp.name) LIKE '%amari duke%'
               OR lower(emp.name) LIKE '%elisha wyatt%'
               OR lower(emp.name) LIKE '%aryanna hess%'
               OR lower(emp.name) LIKE '%bryanna jones%'
               OR lower(emp.name) LIKE '%trace gilmore%'
               OR lower(emp.name) LIKE '%antwan stevenson%'
               OR lower(emp.name) LIKE '%julianna callahan%'
               OR lower(emp.name) LIKE '%jaelynn trevino%'
               OR lower(emp.name) LIKE '%kadence bates%'
               OR lower(emp.name) LIKE '%jakobe townsend%'
            """

        private val query30Plus = """
           1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1
        """

        private val querySimple = """
            SELECT a FROM t
        """

        private val queryNestedSelect = """
            SELECT
                (
                    SELECT a AS p
                    FROM (
                        SELECT VALUE b
                        FROM some_table
                        WHERE 3 = 4
                    ) AS some_wrapped_table
                    WHERE id = 3
                ) AS projectionQuery
            FROM (
                SELECT everything
                FROM (
                    SELECT *
                    FROM someSourceTable AS t
                    LET 5 + t.b AS x
                    WHERE x = 2
                    GROUP BY t.a AS k
                    GROUP AS g
                    ORDER BY t.d
                ) AS someTable
            )
            LET (SELECT a FROM smallTable) AS letVariable
            WHERE letVariable > 4
            GROUP BY t.a AS groupKey
        """

        private val queryList = """
            [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29]
        """

        private val queryFunc = """
            f(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29)
        """

        private val queryFuncInProjection = """
            SELECT
                f(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29)
            FROM t
        """

        private val someJoins = """
           SELECT a
           FROM a, b, c
        """

        private val severalJoins = """
           SELECT a
           FROM a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p 
        """

        private val someProjections = """
           SELECT a, b, c
           FROM t
        """

        private val severalProjections = """
           SELECT a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p
           FROM t
        """

        private val someSelect = """
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t)
        """

        private val severalSelect = """
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t) +
           (SELECT a FROM t)
        """

        private val nestedParen = """
            ((((((((((((((((((((((((((((((0))))))))))))))))))))))))))))))
        """

        private val graphPreFilters = """
        SELECT u as banCandidate
        FROM g
        MATCH (p:Post Where p.isFlagged = true) <-[:createdPost]- (u:Usr WHERE u.isBanned = false AND u.karma < 20) -[:createdComment]->(c:Comment WHERE c.isFlagged = true)
        WHERE p.title LIKE '%considered harmful%'
        """.trimIndent()

        private val graphPattern = """
        SELECT the_a.name AS src, the_b.name AS dest
        FROM my_graph MATCH (the_a:a) -[the_y:y]-> (the_b:b)
        WHERE the_y.score > 10
        """.trimIndent()

        private val manyJoins = """
            SELECT x FROM a INNER CROSS JOIN b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER CROSS JOIN f OUTER JOIN g ON h
        """

        private val timeZone = "TIME WITH TIME ZONE '23:59:59.123456789+18:00'"

        private val caseWhenThen = "CASE WHEN name = 'zoe' THEN 1 WHEN name > 'kumo' THEN 2 ELSE 0 END"

        private val simpleInsert = """
        INSERT INTO foo VALUE 1 AT bar RETURNING MODIFIED OLD bar, MODIFIED NEW bar, ALL NEW *
        """

        private val exceptUnionIntersectSixty = """
            a EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
            EXCEPT a INTERSECT a UNION a
        """

        private val exec20Expressions = """
            EXEC
                a
                b,
                a,
                b,
                c,
                d,
                123,
                "aaaaa",
                'aaaaa',
                @ident,
                1 + 1,
                2 + 2,
                a,
                a,
                a,
                a,
                a,
                a,
                a,
                a
        """

        private val fromLet =
            "SELECT C.region, MAX(nameLength) AS maxLen FROM C LET char_length(C.name) AS nameLength GROUP BY C.region"

        private val groupLimit =
            "SELECT g FROM `[{foo: 1, bar: 10}, {foo: 1, bar: 11}]` AS f GROUP BY f.foo GROUP AS g LIMIT 1"

        private val pivot = """
                    PIVOT foo.a AT foo.b 
                    FROM <<{'a': 1, 'b':'I'}, {'a': 2, 'b':'II'}, {'a': 3, 'b':'III'}>> AS foo
                    LIMIT 1 OFFSET 1
        """.trimIndent()

        private val longFromSourceOrderBy = """
            SELECT *
            FROM [{'a': {'a': 5}}, {'a': {'a': 'b'}}, {'a': {'a': true}}, {'a': {'a': []}}, {'a': {'a': {}}}, {'a': {'a': <<>>}}, {'a': {'a': `{{}}`}}, {'a': {'a': null}}]
            ORDER BY a DESC
        """.trimIndent()

        private val nestedAggregates = """
                SELECT
                    i2 AS outerKey,
                    g2 AS outerGroupAs,
                    MIN(innerQuery.innerSum) AS outerMin,
                    (
                        SELECT VALUE SUM(i2)
                        FROM << 0, 1 >>
                    ) AS projListSubQuery
                FROM (
                    SELECT
                        i,
                        g,
                        SUM(col1) AS innerSum
                    FROM simple_1_col_1_group_2 AS innerFromSource
                    GROUP BY col1 AS i GROUP AS g
                ) AS innerQuery
                GROUP BY innerQuery.i AS i2, innerQuery.g AS g2
        """.trimIndent()

        private val complexQuery = """
            1 + (
                SELECT a, b, c
                FROM [
                    { 'a': 1}
                ] AS t
                LET x AS y
                WHERE y > 2 AND y > 3 AND y > 4
                GROUP BY t.a, t.b AS b, t.c AS c
                GROUP AS d
                ORDER BY x
                LIMIT 1 + 22222222222222222
                OFFSET x + y + z + a + b + c
            ) + (
                CAST(
                    '45678920irufji332r94832fhedjcd2wqbxucri3'
                    AS INT
                )
            ) + [
                1, 2, 3, 4, 5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5
            ] - ((((((((((2)))))))))) + (
                SELECT VALUE { 'a': a } FROM t WHERE t.a > 3
            )
        """.trimIndent()

        private val complexQuery01 = """
            SELECT
            DATE_FORMAT(co.order_date, '%Y-%m') AS order_month,
            DATE_FORMAT(co.order_date, '%Y-%m-%d') AS order_day,
            COUNT(DISTINCT co.order_id) AS num_orders,
            COUNT(ol.book_id) AS num_books,
            SUM(ol.price) AS total_price
            FROM cust_order co
            INNER JOIN order_line ol ON co.order_id = ol.order_id
            GROUP BY 
              DATE_FORMAT(co.order_date, '%Y-%m'),
              DATE_FORMAT(co.order_date, '%Y-%m-%d')
            ORDER BY co.order_date ASC;
        """.trimIndent()

        private val complexQuery02 = """
            SELECT
            c.calendar_date,
            c.calendar_year,
            c.calendar_month,
            c.calendar_dayName,
            COUNT(DISTINCT sub.order_id) AS num_orders,
            COUNT(sub.book_id) AS num_books,
            SUM(sub.price) AS total_price,
            SUM(COUNT(sub.book_id)) AS running_total_num_books,
            LAG(COUNT(sub.book_id), 7) AS prev_books
            FROM calendar_days c
            LEFT JOIN (
                SELECT
                co.order_date,
                co.order_id,
                ol.book_id,
                ol.price
                FROM cust_order co
                INNER JOIN order_line ol ON co.order_id = ol.order_id
            ) sub ON c.calendar_date = sub.order_date
            GROUP BY c.calendar_date, c.calendar_year, c.calendar_month, c.calendar_dayname
            ORDER BY c.calendar_date ASC;
        """.trimIndent()

        private val veryLongQuery = """
            SELECT
              e.employee_id AS "Employee#", e.first_name || '' || e.last_name AS "Name", e.email AS "Email",
              e.phone_number AS "Phone", TO_CHAR(e.hire_date, 'MM/DD/YYYY') AS "Hire Date",
              TO_CHAR(e.salary, 'L99G999D99', 'NLS_NUMERIC_CHARACTERS=''.,''NLS_CURRENCY=''${'$'}''') AS "Salary",
              e.commission_pct AS "Comission%",
              'works as' || j.job_title || 'in' || d.department_name || ' department (manager: '
                || dm.first_name || '' || dm.last_name || ')andimmediatesupervisor:' || m.first_name || '' || m.last_name AS "CurrentJob",
              TO_CHAR(j.min_salary, 'L99G999D99', 'NLS_NUMERIC_CHARACTERS=''.,''NLS_CURRENCY=''${'$'}''') || '-' ||
                  TO_CHAR(j.max_salary, 'L99G999D99', 'NLS_NUMERIC_CHARACTERS=''.,''NLS_CURRENCY=''${'$'}''') AS "CurrentSalary",
              l.street_address || ',' || l.postal_code || ',' || l.city || ',' || l.state_province || ','
                || c.country_name || '(' || r.region_name || ')' AS "Location",
              jh.job_id AS "HistoryJobID",
              'worked from' || TO_CHAR(jh.start_date, 'MM/DD/YYYY') || 'to' || TO_CHAR(jh.end_date, 'MM/DD/YYYY') ||
                'as' || jj.job_title || 'in' || dd.department_name || 'department' AS "HistoryJobTitle"
            FROM employees e
              JOIN jobs j 
                ON e.job_id = j.job_id
              LEFT JOIN employees m 
                ON e.manager_id = m.employee_id
              LEFT JOIN departments d 
                ON d.department_id = e.department_id
              LEFT JOIN employees dm 
                ON d.manager_id = dm.employee_id
              LEFT JOIN locations l
                ON d.location_id = l.location_id
              LEFT JOIN countries c
                ON l.country_id = c.country_id
              LEFT JOIN regions r
                ON c.region_id = r.region_id
              LEFT JOIN job_history jh
                ON e.employee_id = jh.employee_id
              LEFT JOIN jobs jj
                ON jj.job_id = jh.job_id
              LEFT JOIN departments dd
                ON dd.department_id = jh.department_id

            ORDER BY e.employee_id;
        """.trimIndent()

        private val veryLongQuery01 = """
            SELECT
                id as feedId,
                (IF(groupId > 0, groupId, IF(friendId > 0, friendId, userId))) as wallOwnerId,
                (IF(groupId > 0 or friendId > 0, userId, NULL)) as guestWriterId,
                (IF(groupId > 0 or friendId > 0, userId, NULL)) as guestWriterType,
                case
                    when type = 2 then 1 
                    when type = 1 then IF(media_count = 1, 2, 4) 
                    when type = 5 then IF(media_count = 1, IF(albumName = 'Audio Feeds', 5, 6), 7) 
                    when type = 6 then IF(media_count = 1, IF(albumName = 'Video Feeds', 8, 9), 10) 
                end as contentType, 
                albumId, 
                albumName, 
                addTime, 
                IF(validity > 0,IF((validity - updateTime) / 86400000 > 1,(validity - updateTime) / 86400000, 1),0) as validity, 
                updateTime, 
                status, 
                location, 
                latitude as locationLat, 
                longitude as locationLon, 
                sharedFeedId as parentFeedId, 
                case  
                    when privacy = 2 or privacy = 10 then 15 
                    when privacy = 3 then 25 
                    else 1 
                end as privacy, 
                pagefeedcategoryid, 
                case   
                    when lastSharedFeedId = 2 then 10 
                    when lastSharedFeedId = 3 then 15 
                    when lastSharedFeedId = 4 then 25 
                    when lastSharedFeedId = 5 then 20 
                    when lastSharedFeedId = 6 then 99 
                    else 1 
                end as wallOwnerType, 
                (ISNULL(latitude) or latitude = 9999.0 or ISNULL(longitude) or longitude = 9999.0) as latlongexists, 
                (SELECT concat('[',GROUP_CONCAT(moodId),']') FROM feedactivities WHERE newsFeedId = newsfeed.id) as feelings, 
                (SELECT concat('[',GROUP_CONCAT(userId),']') FROM feedtags WHERE newsFeedId = newsfeed.id) as withTag, 
                (SELECT concat('{',GROUP_CONCAT(pos,':', friendId),'}') FROM statustags WHERE newsFeedId = newsfeed.id) as textTag, 
                albumType, 
                defaultCategoryType, 
                linkType,linkTitle,linkURL,linkDesc,linkImageURL,linkDomain, -- Link Content 
                title,description,shortDescription,newsUrl,externalUrlOption, -- Additional Content 
                url, height, width, thumnail_url, thumnail_height, thumbnail_width, duration, artist -- Media 
                FROM  
                (newsfeed LEFT JOIN 
                    (            
                        SELECT  
                        case
                            when (mediaalbums.media_type = 1 and album_name = 'AudioFeeds')
                                or (mediaalbums.media_type = 2 and album_name = 'VideoFeeds')
                            then -1 * mediaalbums.user_id else mediaalbums.id
                        end as albumId, 
                        album_name as albumName, 
                        newsFeedId, 
                        (NULL) as height, 
                        (NULL) as width, 
                        media_thumbnail_url as thumnail_url, 
                        max(thumb_image_height) as thumnail_height, 
                        max(thumb_image_width) as thumbnail_width, 
                        max(media_duration) as duration, 
                        case
                            when mediaalbums.media_type = 1 and album_name = 'AudioFeeds'
                            then 4
                            when mediaalbums.media_type = 2 and album_name = 'VideoFeeds'
                            then 5 else 8
                        end as albumType, 
                        count(mediacontents.id) as media_count,  
                        media_artist as artist  
                        FROM
                        (mediaalbums INNER JOIN mediacontents ON mediaalbums.id = mediacontents.album_id)
                        INNER JOIN newsfeedmediacontents
                        ON newsfeedmediacontents.contentId = mediacontents.id group by newsfeedid 
                        UNION 
                        SELECT   
                        -1 * userId as albumId,  
                        newsFeedId,imageUrl as url, 
                        max(imageHeight) as height, 
                        max(imageWidth) as width, 
                        (NULL) as thumnail_url, 
                        (NULL) as thumnail_height, 
                        (NULL) as thumbnail_width, 
                        (NULL) as duration, 
                        case
                            when albumId = 'default' then 1
                            when albumId = 'profileimages' then 2
                            when albumId = 'coverimages' then 3
                        end as albumType, 
                        count(imageid) as media_count,  
                        (NULL) as artist  
                        FROM userimages
                        INNER JOIN newsfeedimages on userimages.id = newsfeedimages.imageId
                        group by newsfeedid 
                    ) album 
                ON newsfeed.id = album.newsfeedId 
            ) 
            LEFT JOIN 
            ( 
            select newsPortalFeedId as feedid,
            title,description,shortDescription,newsUrl,externalUrlOption, newsPortalCategoryId as pagefeedcategoryid,
            (15) as defaultCategoryType from newsportalFeedInfo 
            UNION 
            select businessPageFeedId as feedid,title,description,shortDescription,newsUrl,externalUrlOption,
            businessPageCategoryId as pagefeedcategoryid,(25) as defaultCategoryType from businessPageFeedInfo 
            UNION 
            select newsfeedId as feedid,(NULL) as title,description,(NULL) as shortDescription,(NULL) as newsUrl,
            (NULL) as externalUrlOption, categoryMappingId as pagefeedcategoryid,
            (20) as defaultCategoryType from mediaPageFeedInfo 
            ) page 
            ON newsfeed.id = page.feedId WHERE privacy != 10 
        """.trimIndent()

        val queries = listOf(
            querySimple, // 0
            nestedParen, // 1
            someJoins, // 2
            severalJoins, // 3
            someSelect, // 4
            severalSelect, // 5
            someProjections, // 6
            severalProjections, // 7
            nestedParen, // 8
            queryFunc, // 9
            queryFuncInProjection, // 10
            queryList, // 11
            query15OrsAndLikes, // 12
            query30Plus, // 13
            queryNestedSelect, // 14
            graphPattern, // 15
            graphPreFilters, // 16
            manyJoins, // 17
            timeZone, // 18
            caseWhenThen, // 19
            simpleInsert, // 20
            exceptUnionIntersectSixty, // 21
            exec20Expressions, // 22
            fromLet, // 23
            groupLimit, // 24
            pivot, // 25
            longFromSourceOrderBy, // 26
            nestedAggregates, // 27
            complexQuery, // 28
            complexQuery01, // 29
            complexQuery02, // 30
            veryLongQuery, // 31
            veryLongQuery01, // 32
        )

        val queriesFailingHalfway = queries.map { query ->
            val splitQuery = query.split("\\s".toRegex()).toMutableList()
            val index = (splitQuery.lastIndex * .50).toInt()
            splitQuery.add(index, ";")
            splitQuery.joinToString(separator = " ")
        }
    }
}
