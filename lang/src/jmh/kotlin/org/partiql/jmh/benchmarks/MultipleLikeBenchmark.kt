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
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.util.concurrent.TimeUnit

/**
 * JMH micro-benchmark for parse/compile/eval of multiple `LIKE` expressions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class MultipleLikeBenchmark {

    companion object {
        private const val FORK_VALUE: Int = FORK_VALUE_RECOMMENDED
        private const val MEASUREMENT_ITERATION_VALUE: Int = MEASUREMENT_ITERATION_VALUE_RECOMMENDED
        private const val MEASUREMENT_TIME_VALUE: Int = MEASUREMENT_TIME_VALUE_RECOMMENDED
        private const val WARMUP_ITERATION_VALUE: Int = WARMUP_ITERATION_VALUE_RECOMMENDED
        private const val WARMUP_TIME_VALUE: Int = WARMUP_TIME_VALUE_RECOMMENDED
    }

    @State(Scope.Thread)
    open class MyState {
        val ion = IonSystemBuilder.standard().build()
        val parser = PartiQLParserBuilder().ionSystem(ion).build()
        val pipeline = CompilerPipeline.standard(ion)

        val name1 = listOf(
            "Bob",
            "Madden",
            "Brycen",
            "Bryanna",
            "Zayne",
            "Jocelynn",
            "Breanna",
            "Margaret",
            "Jasmine",
            "Kenyon",
            "Aryanna",
            "Zackery",
            "Jorden",
            "Malia",
            "Raven",
            "Neveah",
            "Finley",
            "Austin",
            "Jaxson",
            "Tobias",
            "Dominique",
            "Devan",
            "Colby",
            "Tanner",
            "Mckenna",
            "Kristina",
            "Cristal",
            "River",
            "Taliyah",
            "Abagail",
            "Spencer",
            "Gage",
            "Ronnie",
            "Amari",
            "Jabari",
            "Alanna",
            "Anderson",
            "Saniya",
            "Baylee",
            "Elisa",
            "Savannah",
            "Jakobe",
            "Sandra",
            "Simone",
            "Frank",
            "Braedon",
            "Clark",
            "Francisco",
            "Roman",
            "Matias",
            "Messi",
            "Elisha",
            "Alexander",
            "Kadence",
            "Karsyn",
            "Adonis",
            "Ishaan",
            "Trevon",
            "Ryan",
            "Jaelynn",
            "Marilyn",
            "Emma",
            "Avah",
            "Jordan",
            "Riley",
            "Amelie",
            "Denisse",
            "Darion",
            "Lydia",
            "Marley",
            "Brogan",
            "Trace",
            "Maeve",
            "Elijah",
            "Kareem",
            "Erick",
            "Hope",
            "Elisabeth",
            "Antwan",
            "Francesca",
            "Layla",
            "Jase",
            "Angel",
            "Addyson",
            "Mckinley",
            "Julianna",
            "Winston",
            "Royce",
            "Paola",
            "Issac",
            "Zachary",
            "Niko",
            "Shania",
            "Colin",
            "Jesse",
            "Pedro",
            "Cheyenne",
            "Ashley",
            "Karli",
            "Bianca",
            "Mario"
        )
        val name2 = listOf(
            "Smith",
            "Oconnell",
            "Whitehead",
            "Carrillo",
            "Parrish",
            "Monroe",
            "Summers",
            "Hurst",
            "Durham",
            "Hardin",
            "Hunt",
            "Mitchell",
            "Pennington",
            "Woodward",
            "Franklin",
            "Martinez",
            "Shepard",
            "Khan",
            "Mcfarland",
            "Frey",
            "Mckenzie",
            "Blair",
            "Mercer",
            "Callahan",
            "Cameron",
            "Gilmore",
            "Bowers",
            "Donovan",
            "Meyers",
            "Horne",
            "Rice",
            "Castillo",
            "Cain",
            "Dickson",
            "Valenzuela",
            "Silva",
            "Prince",
            "Vance",
            "Berry",
            "Coffey",
            "Young",
            "Walker",
            "Burch",
            "Ross",
            "Mejia",
            "Zuniga",
            "Haney",
            "Jordan",
            "Love",
            "Larsen",
            "Bowman",
            "Werner",
            "Greer",
            "Krause",
            "Bishop",
            "Day",
            "Luna",
            "Patrick",
            "Adkins",
            "Benson",
            "Mcconnell",
            "Sanchez",
            "Villa",
            "Wu",
            "Duke",
            "Fisher",
            "Hess",
            "Lawrence",
            "Perry",
            "Hardy",
            "Wyatt",
            "Mcknight",
            "Thomas",
            "Trevino",
            "Flowers",
            "Cisneros",
            "Coleman",
            "Sanders",
            "Good",
            "Newton",
            "Carpenter",
            "Garza",
            "Barber",
            "Swanson",
            "Owen",
            "Anderson",
            "Bright",
            "Beck",
            "Lawson",
            "Jones",
            "Davila",
            "Porter",
            "Dougherty",
            "Stevenson",
            "Malone",
            "Garrison",
            "Bates",
            "Wheeler",
            "Petty",
            "Rojas",
            "Townsend",
        )

        // cartesian product of name1 x name2 (e.g., listOf("Bob Smith", ... "Mario Townsend"))
        val combined = name1.flatMap { n1 -> name2.map { n2 -> n1 + " " + n2 } }
        var nextId = 1
        val random = kotlin.random.Random(42)
        private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf(' ')
        val employeeData = combined.map { name ->
            val prefix =
                (1..random.nextInt(5, 100)).map { kotlin.random.Random.nextInt(0, charPool.size) }.map(charPool::get)
                    .joinToString("")
            val suffix =
                (1..random.nextInt(5, 100)).map { kotlin.random.Random.nextInt(0, charPool.size) }.map(charPool::get)
                    .joinToString("")
            val id = nextId++
            "{ 'id': $id, 'name': '$prefix $name $suffix' }"
        }

        val data = """
            { 
                'hr': { 
                    'employees': <<""" + employeeData.joinToString(",") + """>>
                } 
            }
        """.trimIndent()

        val bindings = pipeline.compile(parser.parseAstStatement(data)).eval(EvaluationSession.standard()).bindings
        val session = EvaluationSession.build { globals(bindings) }

        val employeeData10 = employeeData + employeeData + employeeData + employeeData + employeeData +
            employeeData + employeeData + employeeData + employeeData + employeeData
        val data10 = """
            { 
                'hr': { 
                    'employees': <<""" + employeeData10.joinToString(",") + """>>
                } 
            }
        """.trimIndent()

        val bindings10 = pipeline.compile(parser.parseAstStatement(data10)).eval(EvaluationSession.standard()).bindings
        val session10 = EvaluationSession.build { globals(bindings10) }

        val query15 = """
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
        val astStatement15 = parser.parseAstStatement(query15)
        val expression15 = pipeline.compile(astStatement15)

        val query30 = query15 + """
               OR lower(emp.name) LIKE '%austin pennington%'
               OR lower(emp.name) LIKE '%colby woodward%'
               OR lower(emp.name) LIKE '%brycen blair%'
               OR lower(emp.name) LIKE '%cristal mercer%'
               OR lower(emp.name) LIKE '%river gilmore%'
               OR lower(emp.name) LIKE '%saniya bowers%'
               OR lower(emp.name) LIKE '%braedon ross%'
               OR lower(emp.name) LIKE '%clark mejia%'
               OR lower(emp.name) LIKE '%ryan day%'
               OR lower(emp.name) LIKE '%marilyn luna%'
               OR lower(emp.name) LIKE '%avah sanchez%'
               OR lower(emp.name) LIKE '%amelie wu%'
               OR lower(emp.name) LIKE '%paola duke%'
               OR lower(emp.name) LIKE '%jesse trevino%'
               OR lower(emp.name) LIKE '%bianca cisneros%'
            """
        val astStatement30 = parser.parseAstStatement(query30)
        val expression30 = pipeline.compile(astStatement30)
    }

    /**
     * Benchmarks parsing a query containing 15 `OR`ed `LIKE` expressions
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLParser15(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.query15)
        blackhole.consume(expr)
    }

    /**
     * Benchmarks compiling a query containing 15 `OR`ed `LIKE` expressions
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLCompiler15(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.astStatement15)
        blackhole.consume(exprValue)
    }

    /**
     * Benchmarks evaluating a query containing 15 `OR`ed `LIKE` expressions
     * against 10,201 rows of strings each of which are ~20 to ~220 codepoints long
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLEvaluator15(state: MyState, blackhole: Blackhole) {
        val exprValue = state.expression15.eval(state.session)
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    /**
     * Benchmarks parsing a query containing 30 `OR`ed `LIKE` expressions
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLParser30(state: MyState, blackhole: Blackhole) {
        val expr = state.parser.parseAstStatement(state.query30)
        blackhole.consume(expr)
    }

    /**
     * Benchmarks compiling a query containing 30 `OR`ed `LIKE` expressions
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLCompiler30(state: MyState, blackhole: Blackhole) {
        val exprValue = state.pipeline.compile(state.astStatement30)
        blackhole.consume(exprValue)
    }

    /**
     * Benchmarks evaluating a query containing 30 `OR`ed `LIKE` expressions
     * against 10,201 rows of strings each of which are ~20 to ~220 codepoints long
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLEvaluator30(state: MyState, blackhole: Blackhole) {
        val exprValue = state.expression30.eval(state.session)
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }

    /**
     * Benchmarks evaluating a query containing 15 `OR`ed `LIKE` expressions
     * against 102,010 rows of strings each of which are ~20 to ~220 codepoints long
     */
    @Benchmark
    @Fork(value = FORK_VALUE)
    @Measurement(iterations = MEASUREMENT_ITERATION_VALUE, time = MEASUREMENT_TIME_VALUE)
    @Warmup(iterations = WARMUP_ITERATION_VALUE, time = WARMUP_TIME_VALUE)
    fun testPartiQLEvaluator30WithData10(state: MyState, blackhole: Blackhole) {
        val exprValue = state.expression30.eval(state.session10)
        blackhole.consume(exprValue)
        blackhole.consume(exprValue.iterator().forEach { })
    }
}
