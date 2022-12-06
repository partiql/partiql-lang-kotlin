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

package org.partiql.jmh.eval.builtins

import com.amazon.ion.system.IonSystemBuilder
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.builtins.MathFunctions

/**
 * This class establishes benchmarks for out builtin functions.
 *
 * https://github.com/partiql/partiql-docs/blob/main/docs/builtins-sql.adoc
 */
open class BuiltinsBenchmark {

    @State(Scope.Benchmark)
    open class BenchmarkState {

        val valueFactory = ExprValueFactory.standard(IonSystemBuilder.standard().build())
        val sess = EvaluationSession.standard()

        val ceil: ExprFunction = MathFunctions.createCeil(valueFactory)

        // explicitly not val/constants
        var zero = 0.0
        var half = 0.5
        var nan = Double.NaN
        var `+inf` = Double.POSITIVE_INFINITY
        var `-inf` = Double.NEGATIVE_INFINITY

    }

    //--- Numeric ----------------------

    // fun abs(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // fun mod(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    fun ceil(state: BenchmarkState, blackhole: Blackhole) {
        val zero = state.valueFactory.newFloat(state.zero)
        val half = state.valueFactory.newFloat(state.half)
        val nan = state.valueFactory.newFloat(state.nan)
        val `+inf` = state.valueFactory.newFloat(state.`+inf`)
        val `-inf` = state.valueFactory.newFloat(state.`-inf`)
        val r0 = state.ceil.callWithRequired(state.sess, listOf(zero))
        val r1 = state.ceil.callWithRequired(state.sess, listOf(half))
        val r2 = state.ceil.callWithRequired(state.sess, listOf(nan))
        val r3 = state.ceil.callWithRequired(state.sess, listOf(`+inf`))
        val r4 = state.ceil.callWithRequired(state.sess, listOf(`-inf`))
        blackhole.consume(r0)
        blackhole.consume(r1)
        blackhole.consume(r2)
        blackhole.consume(r3)
        blackhole.consume(r4)
    }

    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun floor(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // fun sqrt(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // fun exp(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // fun power(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // fun ln(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // //--- Strings ------------------------
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun concat(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun lower(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun upper(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun bitLength(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun charLength(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // fun octetLength(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun substring(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // fun substringPattern(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun trim(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun trimLeading(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun trimTrailing(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun trimChars(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun trimCharsLeading(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // @Benchmark
    // @BenchmarkMode(Mode.Throughput)
    // fun trimCharsTrailing(state: BenchmarkState, blackhole: Blackhole) {
    //
    // }
    //
    // fun position(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // fun overlay(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }
    //
    // fun overlayFor(state: BenchmarkState, blackhole: Blackhole) {
    //     // MISSING
    // }

}
