package org.partiql.jmh.benchmarks

import com.amazon.ion.system.IonSystemBuilder
import kotlinx.coroutines.runBlocking
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
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.jmh.utils.FORK_VALUE_RECOMMENDED
import org.partiql.jmh.utils.MEASUREMENT_ITERATION_VALUE_RECOMMENDED
import org.partiql.jmh.utils.MEASUREMENT_TIME_VALUE_RECOMMENDED
import org.partiql.jmh.utils.WARMUP_ITERATION_VALUE_RECOMMENDED
import org.partiql.jmh.utils.WARMUP_TIME_VALUE_RECOMMENDED
import org.partiql.lang.compiler.PartiQLCompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Deprecated("To be removed in the next major version once the synchronous physical plan compiler is removed.")
open class PartiQLCompilerPipelineBenchmark {
    companion object {
        private const val FORK_VALUE: Int = FORK_VALUE_RECOMMENDED
        private const val MEASUREMENT_ITERATION_VALUE: Int = MEASUREMENT_ITERATION_VALUE_RECOMMENDED
        private const val MEASUREMENT_TIME_VALUE: Int = MEASUREMENT_TIME_VALUE_RECOMMENDED
        private const val WARMUP_ITERATION_VALUE: Int = WARMUP_ITERATION_VALUE_RECOMMENDED
        private const val WARMUP_TIME_VALUE: Int = WARMUP_TIME_VALUE_RECOMMENDED
    }

    @State(Scope.Thread)
    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
    open class MyState {
        val parser = PartiQLParserBuilder.standard().build()
        val myIonSystem = IonSystemBuilder.standard().build()

        fun tableWithRows(numRows: Int): ExprValue {
            val allRows = (1..numRows).joinToString { index ->
                """
                    {
                        "id": $index,
                        "someString": "some string foo $index",
                        "someDecimal": $index.00,
                        "someBlob": {{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }},
                        "someTimestamp": 2007-02-23T12:14:15.${index}Z
                    }
                """.trimIndent()
            }
            val data = "[ $allRows ]"
            return ExprValue.of(
                myIonSystem.singleValue(data)
            )
        }

        val bindings = Bindings.ofMap(
            mapOf(
                "t1" to tableWithRows(1),
                "t10" to tableWithRows(10),
                "t100" to tableWithRows(100),
                "t1000" to tableWithRows(1000),
                "t10000" to tableWithRows(10000),
                "t100000" to tableWithRows(100000),
            )
        )

        val parameters = listOf(
            ExprValue.newInt(5), // WHERE `id` > 5
            ExprValue.newInt(1000000), // LIMIT 1000000
            ExprValue.newInt(3), // OFFSET 3 * 2
            ExprValue.newInt(2), // ------------^
        )
        val session = EvaluationSession.build {
            globals(bindings)
            parameters(parameters)
        }

        val pipeline = PartiQLCompilerPipeline.build {
            planner.globalVariableResolver {
                val value = session.globals[it]
                if (value != null) {
                    GlobalResolutionResult.GlobalVariable(it.name)
                } else {
                    GlobalResolutionResult.Undefined
                }
            }
        }

        val query1 = parser.parseAstStatement(
            """
            SELECT * FROM t100000
            """.trimIndent()
        )
        val query2 = parser.parseAstStatement(
            """
            SELECT *
            FROM t100000
            WHERE t100000.someTimestamp < UTCNOW()
            """.trimIndent()
        )
        val query3 = parser.parseAstStatement(
            """
            SELECT *
            FROM t100000
            WHERE t100000.someTimestamp < UTCNOW()
            LIMIT ${Int.MAX_VALUE}
            """.trimIndent()
        )
        val query4 = parser.parseAstStatement(
            """
            SELECT *
            FROM t100000
            WHERE t100000.someTimestamp < UTCNOW()
            ORDER BY t100000.id DESC
            """.trimIndent()
        )
        val query5 = parser.parseAstStatement(
            """
            SELECT *
            FROM t100000
            WHERE t100000.someTimestamp < UTCNOW() AND t100000.id > ?
            LIMIT ?
            OFFSET ? * ?
            """.trimIndent()
        )
        val query6 = parser.parseAstStatement(
            """
            SELECT *
            FROM t100000
            WHERE t100000.someTimestamp < UTCNOW() AND t100000.id > ?
            ORDER BY t100000.id DESC
            LIMIT ?
            OFFSET ? * ?
            """.trimIndent()
        )
        val query7 = parser.parseAstStatement(
            """
            SELECT *
            FROM t10000
            WHERE t10000.someTimestamp < UTCNOW() AND t10000.id > ?
            ORDER BY t10000.id DESC
            LIMIT ?
            OFFSET ? * ?
            """.trimIndent()
        )
        val query8 = parser.parseAstStatement(
            """
            SELECT *
            FROM t1000
            WHERE t1000.someTimestamp < UTCNOW() AND t1000.id > ?
            ORDER BY t1000.id DESC
            LIMIT ?
            OFFSET ? * ?
            """.trimIndent()
        )
        val query9 = parser.parseAstStatement(
            """
            SELECT *
            FROM t100
            WHERE t100.someTimestamp < UTCNOW() AND t100.id > ?
            ORDER BY t100.id DESC
            LIMIT ?
            OFFSET ? * ?
            """.trimIndent()
        )
        val query10 = parser.parseAstStatement(
            """
            SELECT *
            FROM t10
            WHERE t10.someTimestamp < UTCNOW() AND t10.id > ?
            ORDER BY t10.id DESC
            LIMIT ?
            OFFSET ? * ?
            """.trimIndent()
        )
        val query11 = parser.parseAstStatement(
            """
            SELECT *
            FROM t1
            WHERE t1.someTimestamp < UTCNOW() AND t1.id > ?
            ORDER BY t1.id DESC
            LIMIT ?
            OFFSET ? * ?
            """.trimIndent()
        )

        val statement1 = pipeline.compile(query1)
        val statement2 = pipeline.compile(query2)
        val statement3 = pipeline.compile(query3)
        val statement4 = pipeline.compile(query4)
        val statement5 = pipeline.compile(query5)
        val statement6 = pipeline.compile(query6)
        val statement7 = pipeline.compile(query7)
        val statement8 = pipeline.compile(query8)
        val statement9 = pipeline.compile(query9)
        val statement10 = pipeline.compile(query10)
        val statement11 = pipeline.compile(query11)
    }

    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testCompileQuery1(state: MyState, blackhole: Blackhole) = runBlocking {
        val statement = state.pipeline.compile(state.query1)
        blackhole.consume(statement)
    }

    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testCompileQuery2(state: MyState, blackhole: Blackhole) = runBlocking {
        val statement = state.pipeline.compile(state.query2)
        blackhole.consume(statement)
    }

    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testCompileQuery3(state: MyState, blackhole: Blackhole) = runBlocking {
        val statement = state.pipeline.compile(state.query3)
        blackhole.consume(statement)
    }

    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testCompileQuery4(state: MyState, blackhole: Blackhole) {
        val statement = state.pipeline.compile(state.query4)
        blackhole.consume(statement)
    }

    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testCompileQuery5(state: MyState, blackhole: Blackhole) {
        val statement = state.pipeline.compile(state.query5)
        blackhole.consume(statement)
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery1(state: MyState, blackhole: Blackhole) {
        val result = state.statement1.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery2(state: MyState, blackhole: Blackhole) {
        val result = state.statement2.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery3(state: MyState, blackhole: Blackhole) {
        val result = state.statement3.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery4(state: MyState, blackhole: Blackhole) {
        val result = state.statement4.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery5(state: MyState, blackhole: Blackhole) {
        val result = state.statement5.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery6(state: MyState, blackhole: Blackhole) {
        val result = state.statement6.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery7(state: MyState, blackhole: Blackhole) {
        val result = state.statement7.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery8(state: MyState, blackhole: Blackhole) {
        val result = state.statement8.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery9(state: MyState, blackhole: Blackhole) {
        val result = state.statement9.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery10(state: MyState, blackhole: Blackhole) {
        val result = state.statement10.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testEvalQuery11(state: MyState, blackhole: Blackhole) {
        val result = state.statement11.eval(state.session)
        val exprValue = (result as PartiQLResult.Value).value
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }
}
