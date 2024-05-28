package org.partiql.planner.internal.typer.operator

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.parser.PartiQLParser
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.typer.PartiQLTyperTestBase
import org.partiql.planner.internal.typer.PlanTyperTestsPorted
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.ProblemCollector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.types.StaticType

class OpCastTest : PartiQLTyperTestBase() {

    private val testProvider = PartiQLTestProvider()

    init {
        // load test inputs
        testProvider.load()
    }

    @ParameterizedTest
    @MethodSource("casts")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCasts(tc: PlanTyperTestsPorted.TestCase.SuccessTestCase) = runTest(tc)

    companion object {

        private val parser = PartiQLParser.default()
        private val planner = PartiQLPlanner.default()

        val catalogs: List<Pair<String, ConnectorMetadata>> = emptyList()

        private fun key(name: String) = PartiQLTest.Key("basics", name)

        @JvmStatic
        fun casts() = listOf(
            PlanTyperTestsPorted.TestCase.SuccessTestCase(
                key = key("cast-00"),
                expected = StaticType.BOOL,
            ),
        )
    }

    private fun infer(
        query: String,
        session: PartiQLPlanner.Session,
        problemCollector: ProblemCollector,
    ): PartiQLPlan {
        val ast = parser.parse(query).root
        return planner.plan(ast, session, problemCollector).plan
    }

    private fun runTest(tc: PlanTyperTestsPorted.TestCase.SuccessTestCase) {
        val session = PartiQLPlanner.Session(
            tc.query.hashCode().toString(),
            "USER_ID",
            tc.catalog,
            tc.catalogPath,
            catalogs = mapOf(*catalogs.toTypedArray())
        )

        val hasQuery = tc.query != null
        val hasKey = tc.key != null
        if (hasQuery == hasKey) {
            error("Test must have one of either `query` or `key`")
        }
        val input = tc.query ?: testProvider[tc.key!!]!!.statement

        val collector = ProblemCollector()
        val plan = infer(input, session, collector)
        when (val statement = plan.statement) {
            is Statement.Query -> {
                assert(collector.problems.isEmpty()) {
                    buildString {
                        appendLine(collector.problems.toString())
                        appendLine()
                        PlanPrinter.append(this, statement)
                    }
                }
                val actual = statement.root.type
                assert(tc.expected == actual) {
                    buildString {
                        appendLine()
                        appendLine("Expect: ${tc.expected}")
                        appendLine("Actual: $actual")
                        appendLine()
                        PlanPrinter.append(this, statement)
                    }
                }
            }
        }
    }
}
