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
import org.partiql.jmh.utils.FORK_VALUE_RECOMMENDED
import org.partiql.jmh.utils.MEASUREMENT_ITERATION_VALUE_RECOMMENDED
import org.partiql.jmh.utils.MEASUREMENT_TIME_VALUE_RECOMMENDED
import org.partiql.jmh.utils.WARMUP_ITERATION_VALUE_RECOMMENDED
import org.partiql.jmh.utils.WARMUP_TIME_VALUE_RECOMMENDED
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
internal open class LiteralBenchmark {
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
    fun parseNull(state: MyState) {
        state.parser.parseAstStatement("NULL")
        state.parser.parseAstStatement("`null`")
        state.parser.parseAstStatement("`null.int`")
        state.parser.parseAstStatement("`null.string`")
        state.parser.parseAstStatement("`null.list`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonNull(state: MyState) {
        state.parser.parseAstStatement("`null`")
        state.parser.parseAstStatement("`null.int`")
        state.parser.parseAstStatement("`null.string`")
        state.parser.parseAstStatement("`null.list`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseMissing(state: MyState) {
        state.parser.parseAstStatement("MISSING")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseBool(state: MyState) {
        state.parser.parseAstStatement("true")
        state.parser.parseAstStatement("`true`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonBool(state: MyState) {
        state.parser.parseAstStatement("false")
        state.parser.parseAstStatement("`false`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIntegerLiterals(state: MyState) {
        state.parser.parseAstStatement("1")
        state.parser.parseAstStatement("-1")
        state.parser.parseAstStatement("${Long.MAX_VALUE}")
        state.parser.parseAstStatement("${Long.MIN_VALUE}")
        state.parser.parseAstStatement("321837")
        state.parser.parseAstStatement("-938472")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLargeIntegerLiteral(state: MyState) {
        state.parser.parseAstStatement(state.largeInt)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonIntegerLiterals(state: MyState) {
        state.parser.parseAstStatement("`1`")
        state.parser.parseAstStatement("`-1`")
        state.parser.parseAstStatement("`${Long.MAX_VALUE}`")
        state.parser.parseAstStatement("`${Long.MIN_VALUE}`")
        state.parser.parseAstStatement("`321837`")
        state.parser.parseAstStatement("`-938472`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLargeIonIntegerLiteral(state: MyState) {
        state.parser.parseAstStatement("`${state.largeInt}`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseFloatLiterals(state: MyState) {
        state.parser.parseAstStatement("`0e0`")
        state.parser.parseAstStatement("`123e-2`")
        state.parser.parseAstStatement("`-3229e123`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseDecimalLiterals(state: MyState) {
        state.parser.parseAstStatement("0.")
        state.parser.parseAstStatement("1.1")
        state.parser.parseAstStatement("-1.234")
        state.parser.parseAstStatement("3827.23218")
        state.parser.parseAstStatement("-29387261.382736")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLargeDecimalLit(state: MyState) {
        state.parser.parseAstStatement(state.largeDecimal)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonDecimalLiterals(state: MyState) {
        state.parser.parseAstStatement("`0.`")
        state.parser.parseAstStatement("`1.1`")
        state.parser.parseAstStatement("`-1.234`")
        state.parser.parseAstStatement("`3827.23218`")
        state.parser.parseAstStatement("`-29387261.382736`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonLargeDecimalLiterals(state: MyState) {
        state.parser.parseAstStatement("`${state.largeDecimal}`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseDateLiterals(state: MyState) {
        state.parser.parseAstStatement("DATE '2001-01-12'")
        state.parser.parseAstStatement("DATE '9999-02-13'")
        state.parser.parseAstStatement("DATE '0100-01-01'")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseTimestampLiterals(state: MyState) {
        state.parser.parseAstStatement("`2007-02-23T12:14Z`")
        state.parser.parseAstStatement("`2007-02-23T12:14:33.079-08:00`")
        state.parser.parseAstStatement("`2007-02-23T20:14:33.079Z`")
        state.parser.parseAstStatement("`2007-02-23T20:14:33.079+00:00`")
        state.parser.parseAstStatement("`2007-02-23T20:14:33.079-00:00`")
        state.parser.parseAstStatement("`2007-01-01T00:00-00:00`")
        state.parser.parseAstStatement("`2007-01-01`")
        state.parser.parseAstStatement("`2007-01-01T`")
        state.parser.parseAstStatement("`2007-01T`")
        state.parser.parseAstStatement("`2007T`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseTimeLiteral(state: MyState) {
        state.parser.parseAstStatement("TIME '12:34:56'")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSymbolLiterals(state: MyState) {
        state.parser.parseAstStatement("`a`")
        state.parser.parseAstStatement("`abc`")
        state.parser.parseAstStatement("`abcdefg`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseStringLiterals(state: MyState) {
        state.parser.parseAstStatement("'a'")
        state.parser.parseAstStatement("'abc'")
        state.parser.parseAstStatement("'abcdefg'")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongStringLiteral(state: MyState) {
        state.parser.parseAstStatement("`'${state.longString}'`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonStringLiterals(state: MyState) {
        state.parser.parseAstStatement("`\"a\"`")
        state.parser.parseAstStatement("`\"abc\"`")
        state.parser.parseAstStatement("`\"abcdefg\"`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongIonStringLiteral(state: MyState) {
        state.parser.parseAstStatement("`\"${state.longString}\"`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseClobLiteral(state: MyState) {
        state.parser.parseAstStatement(
            """
            `{{ "This is a CLOB of text." }}`
            """.trimIndent()
        )
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseBlobLiteral(state: MyState) {
        state.parser.parseAstStatement(
            """
            `{{ VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE= }}`
            """.trimIndent()
        )
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseListLiteral(state: MyState) {
        state.parser.parseAstStatement("[]")
        state.parser.parseAstStatement("[1,2,3]")
        state.parser.parseAstStatement("[1,'a',true]")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongListLiteral(state: MyState) {
        state.parser.parseAstStatement(state.longList)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonListLiteral(state: MyState) {
        state.parser.parseAstStatement("`[]`")
        state.parser.parseAstStatement("`[1,2,3]`")
        state.parser.parseAstStatement("`[1,'a',true]`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongIonListLiteral(state: MyState) {
        state.parser.parseAstStatement("`${state.longList}`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseSexpLiteral(state: MyState) {
        state.parser.parseAstStatement("`()`")
        state.parser.parseAstStatement("`(1 2 3)`")
        state.parser.parseAstStatement("`(1 'a' true)`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongIonSexpLiteral(state: MyState) {
        state.parser.parseAstStatement(state.longSexp)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseBagLiteral(state: MyState) {
        state.parser.parseAstStatement("<<>>")
        state.parser.parseAstStatement("<<1,2,3>>")
        state.parser.parseAstStatement("<<1,'a',true>>")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongBagLiteral(state: MyState) {
        state.parser.parseAstStatement(state.longBag)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseStructLiteral(state: MyState) {
        state.parser.parseAstStatement("{}")
        state.parser.parseAstStatement("{'a':1,'b':2,'c':3}")
        state.parser.parseAstStatement("{'a':1,'b':2.0,'c':'a','d':true}")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongStructLiteral(state: MyState) {
        state.parser.parseAstStatement(state.longStruct)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseIonStructLiteral(state: MyState) {
        state.parser.parseAstStatement("`{}`")
        state.parser.parseAstStatement("`{'a':1,'b':2,'c':3}`")
        state.parser.parseAstStatement("`{'a':1,'b':2.0,'c':'a','d':true}`")
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    @Suppress("UNUSED")
    fun parseLongIonStructLiteral(state: MyState) {
        state.parser.parseAstStatement("`${state.longStruct}`")
    }

    @State(Scope.Thread)
    open class MyState {

        private val ion: IonSystem = IonSystemBuilder.standard().build()
        val parser = PartiQLParserBuilder().ionSystem(ion).build()

        val largeInt = "1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
        val largeDecimal = "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111.11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
        val longString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val longList = "[[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,null,null,null,null,null,null,null,null,null,null,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]]"
        val longSexp = "`((1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0)(1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a'true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0) (1 1 1 1 1 1 1 1 1 1 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' 'a' true true true true true true true true true true a a a a a a a a a a 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0))`"
        val longBag = "<<<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>,<<1,1,1,1,1,1,1,1,1,1,'a','a','a','a','a','a','a','a','a','a',true,true,true,true,true,true,true,true,true,true,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,`a`,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0>>>>"
        val longStruct = "{'f0':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f1':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f2':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f3':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f4':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f5':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f6':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f7':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f8':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0},'f9':{'a0':1,'a1':1,'a2':1,'a3':1,'a4':1,'a5':1,'a6':1,'71':1,'a8':1,'a9':1,'b0':'a','b1':'a','b2':'a','b3':'a','b4':'a','b5':'a','b6':'a','b7':'a','b8':'a','b9':'a','c0':true,'c1':true,'c2':true,'c3':true,'c4':true,'c5':true,'c6':true,'c7':true,'c8':true,'c9':true,'d0':[],'d1':[],'d2':[],'d3':[],'d4':[],'d5':[],'d6':[],'d7':[],'d8':[],'d9':[],'e0':1.0,'e1':1.0,'e2':1.0,'e3':1.0,'e4':1.0,'e5':1.0,'e6':1.0,'e7':1.0,'e8':1.0,'e9':1.0}}"
    }
}
