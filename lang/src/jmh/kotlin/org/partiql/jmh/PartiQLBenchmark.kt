package org.partiql.jmh

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.concurrent.TimeUnit

/**
 * These are the sample benchmarks to demonstrate how JMH benchmarks in PartiQL should be set up.
 * Refer this [JMH tutorial](http://tutorials.jenkov.com/java-performance/jmh.html) for more information on [Benchmark]s,
 * [BenchmarkMode]s, etc.
 */
open class PartiQLBenchmark {
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
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Fork(value = 2)
    fun testPartiQLParser(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.query)
        blackhole.consume(expr)
    }

    /**
     * Example PartiQL benchmark for compiling a query
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Fork(value = 2)
    fun testPartiQLCompiler(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.astStatement)
        blackhole.consume(exprValue)
    }

    /**
     * Example PartiQL benchmark for evaluating a query
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Fork(value = 2)
    fun testPartiQLEvaluator(state: MyState, blackhole: Blackhole) {
        val exprValue = state.expression.eval(state.session)
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }
}
