package org.partiql.jmh

import com.amazon.ion.system.IonSystemBuilder
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
import org.partiql.lang.syntax.SqlParser
import java.util.concurrent.TimeUnit

/**
 * JMH micro-benchmark for parse/compile/eval of multiple `LIKE` expressions.
 */
open class MultipleLikeBenchmark {
    @State(Scope.Thread)
    open class MyState {
        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)
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

        val query = """
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
