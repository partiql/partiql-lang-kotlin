package org.partiql.lang

import com.amazon.ion.system.IonSystemBuilder
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.infra.Blackhole
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.SqlParser
import java.util.concurrent.TimeUnit


/**
 * These are the sample benchmarks to demonstrate how JMH benchmarks in PartiQL should be set up.
 * Refer this [JMH tutorial](http://tutorials.jenkov.com/java-performance/jmh.html) for more information on [Benchmark]s,
 * [BenchmarkMode]s, etc.
 */
open class PartiQLBenchmark {
    @State(Scope.Thread)
    open class MyState {
        var a = 1
        var b = 2
        var sum = 0
    }

    /**
     * Use [State] objects to initialize some variables that your benchmark code needs,
     * but which you do not want to be part of the code your benchmark measures.
     */
    @Benchmark
    fun testMethod(state: MyState) {
        // This is a demo/sample template for building your JMH benchmarks. Edit as needed.
        // Put your benchmark code here.
        state.sum = state.a + state.b
    }

    /**
     * State class with [Setup] and [TearDown] annotations.
     *
     * The [Setup] annotation tell JMH that this method should be called to setup the state object
     * before it is passed to the benchmark method.
     * The [TearDown] annotation tells JMH that this method should be called to
     * clean up ("tear down") the state object after the benchmark has been executed.
     *
     * The setup and tear down execution time is not included in the benchmark runtime measurements.
     */
    @State(Scope.Thread)
    open class MyState2 {
        @Setup(Level.Trial)
        fun doSetup() {
            sum = 0
            println("Do Setup")
        }

        @TearDown(Level.Trial)
        fun doTearDown() {
            println("Do TearDown")
        }

        var a = 1
        var b = 2
        var sum = 0

        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)
        val pipeline = CompilerPipeline.standard(ion)

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
        val bindings = pipeline.compile(parser.parseExprNode(data)).eval(EvaluationSession.standard()).bindings
        val session = EvaluationSession.build { globals(bindings) }

        val query = "SELECT * FROM hr.employeesNestScalars"
        val exprNode = parser.parseExprNode(query)
        val expression = pipeline.compile(exprNode)
    }

    /**
     * You can use different [BenchmarkMode]. The benchmark mode tells JMH what you want to measure.
     * [TimeUnit] specifies what time units you want the benchmark results printed in.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun testMethod2(state: MyState2) {
        state.sum = state.a + state.b
    }

    /**
     * Pass the final values to the [Blackhole] to fool JMH that it is indeed been used.
     */
    @Benchmark
    fun testMethod3(state: MyState, blackhole: Blackhole) {
        val sum1 = state.a + state.b
        val sum2 = state.a + state.a + state.b + state.b
        blackhole.consume(sum1)
        blackhole.consume(sum2)
    }

    /**
     * Example PartiQL benchmark for parsing a query
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    fun testPartiQLParser(state: MyState2, blackhole: Blackhole) {
        val expr = state.parser.parseExprNode(state.query)
        blackhole.consume(expr)
    }

    /**
     * Example PartiQL benchmark for compiling a query
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    fun testPartiQLCompiler(state: MyState2, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.exprNode)
        blackhole.consume(exprValue)
    }

    /**
     * Example PartiQL benchmark for evaluating a query
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    fun testPartiQLEvaluator(state: MyState2, blackhole: Blackhole) {
        val exprValue = state.expression.eval(state.session)
        blackhole.consume(exprValue)
    }
}