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
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.concurrent.TimeUnit

/**
 * These are the sample benchmarks to demonstrate how JMH benchmarks in PartiQL should be set up.
 * Refer this [JMH tutorial](http://tutorials.jenkov.com/java-performance/jmh.html) for more information on [Benchmark]s,
 * [BenchmarkMode]s, etc.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class PartiQLBenchmark {

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
        val pipeline = CompilerPipeline.standard()

        val data = """
            { 
                'hr': { 
                    'employeesNestScalars': <<
                        { 
                            'id': 3, 
                            'name': 'Bob Smith', 
                            'title': null, 
                            'projects': [ 
                                'AWS Redshift Spectrum querying',
                                'AWS Redshift security',
                                'AWS Aurora security'
                            ]
                        },
                        { 
                            'id': 4, 
                            'name': 'Susan Smith', 
                            'title': 'Dev Mgr', 
                            'projects': []
                        },
                        { 
                            'id': 6, 
                            'name': 'Jane Smith', 
                            'title': 'Software Eng 2', 
                            'projects': [ 'AWS Redshift security' ]
                        }
                    >>
                } 
            }
        """.trimIndent()
        val bindings = pipeline.compile(parser.parseAstStatement(data)).eval(EvaluationSession.standard()).bindings
        val session = EvaluationSession.build { globals(bindings) }

        val query = "SELECT * FROM hr.employeesNestScalars"
        val astStatement = parser.parseAstStatement(query)
        val expression = pipeline.compile(astStatement)
    }

    /**
     * Example PartiQL benchmark for parsing a query
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLParser(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.query)
        blackhole.consume(expr)
    }

    /**
     * Example PartiQL benchmark for compiling a query
     */

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLCompiler(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.astStatement)
        blackhole.consume(exprValue)
    }

    /**
     * Example PartiQL benchmark for evaluating a query
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLEvaluator(state: MyState, blackhole: Blackhole) {
        val exprValue = state.expression.eval(state.session)
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }
}
