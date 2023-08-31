package org.partiql.planner

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.Statement
import org.partiql.planner.test.PlannerTest
import org.partiql.planner.test.PlannerTestLog
import org.partiql.planner.test.PlannerTestProvider
import org.partiql.planner.test.PlannerTestSuite
import org.partiql.planner.test.plugin.FsPlugin
import org.partiql.planner.test.toIon
import org.partiql.types.TypingMode
import java.util.stream.Stream
import kotlin.io.path.toPath

class PlannerTestJunit {

    @TestFactory
    fun mapSuitesToJunitTests(): Stream<DynamicNode> {
        val provider = PlannerTestProvider()
        return provider.suites().map { suiteNode(it) }
    }

    companion object {

        private val log = PlannerTestLog(System.out)

        private val parser = PartiQLParserBuilder.standard().build()

        private val catalogConfig = mapOf(
            "default" to ionStructOf(
                field("connector_name", ionString("fs")),
            )
        )

        private fun suiteNode(suite: PlannerTestSuite): DynamicContainer {
            val schemaRoot = PlannerTest::class.java.getResource("/catalogs")!!.toURI().toPath()
            val plugin = FsPlugin(schemaRoot)
            val planner = PartiQLPlannerBuilder()
                .mode(TypingMode.STRICT)
                .plugins(listOf(plugin))
                .build()
            val tests = suite.tests.map { (name, test) ->
                val testName = "${suite.name}__$name"
                val session = PartiQLPlanner.Session(
                    queryId = "q__$testName",
                    userId = "Planner_test_runner",
                    currentCatalog = suite.session.catalog,
                    currentDirectory = suite.session.path,
                    catalogConfig = catalogConfig,
                )
                testNode(testName, planner, session, test)
            }
            return dynamicContainer(suite.name, tests.stream())
        }

        private fun testNode(
            displayName: String,
            planner: PartiQLPlanner,
            session: PartiQLPlanner.Session,
            test: PlannerTest,
        ): DynamicTest {
            return dynamicTest(displayName) {
                val ast = parser.parse(test.statement).root
                val result = planner.plan(ast, session)
                for (problem in result.problems) {
                    if (problem.details.severity == ProblemSeverity.ERROR) {
                        fail { result.problems.joinToString() }
                    }
                }
                val statement = result.plan.statement
                if (statement !is Statement.Query) {
                    fail { "Expected plan statement to be a Statement.Query" }
                }
                val expected = test.schema.toIon()
                val actual = statement.root.type.toIon()
                assertEquals(expected, actual)
            }
        }
    }
}
