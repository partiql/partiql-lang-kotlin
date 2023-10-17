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
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.plugins.local.LocalPlugin
import org.partiql.plugins.local.toIon
import java.util.stream.Stream

/**
 * PlannerTestJunit is responsible for constructing JUnit test suites from all input queries in the testFixtures.
 *
 * I believe this can be more generic and added to testFixtures; but that is outside the scope of current work.
 */
class PlannerTestJunit {

    @TestFactory
    fun mapSuitesToJunitTests(): Stream<DynamicNode> {
        val inputs = PartiQLTestProvider().inputs()
        val cases = PlannerTestProvider().groups()
        return cases.map { groupNode(it, inputs) }
    }

    companion object {

        private val root = PartiQLTestProvider::class.java.getResource("/catalogs")!!.toURI().path

        private val parser = PartiQLParserBuilder.standard().build()

        private val catalogConfig = mapOf(
            "default" to ionStructOf(
                field("connector_name", ionString("local")),
                field("root", ionString("$root/default")),
            ),
            "tpc_ds" to ionStructOf(
                field("connector_name", ionString("local")),
                field("root", ionString("$root/tpc_ds")),
            ),
        )

        private fun groupNode(group: PlannerTestGroup, inputs: Map<String, PartiQLTest>): DynamicContainer {
            val plugin = LocalPlugin()
            val planner = PartiQLPlannerBuilder()
                .plugins(listOf(plugin))
                .build()
            // Map all cases to an input
            val tests = group.cases.map { case ->
                val key = "${group.name}__${case.input}"
                val input = inputs[key]
                // Report bad input mapping
                if (input == null) {
                    return@map failTestNode(key, "Missing input for `$key`")
                }
                val session = PartiQLPlanner.Session(
                    queryId = key,
                    userId = "Planner_test_runner",
                    currentCatalog = case.catalog,
                    currentDirectory = case.catalogPath,
                    catalogConfig = catalogConfig,
                )
                testNode(key, planner, session, input.statement, case)
            }
            return dynamicContainer(group.name, tests.stream())
        }

        private fun failTestNode(id: String, message: String): DynamicTest {
            return dynamicTest(id) {
                fail { message }
            }
        }

        private fun testNode(
            displayName: String,
            planner: PartiQLPlanner,
            session: PartiQLPlanner.Session,
            statement: String,
            case: PlannerTestCase,
        ): DynamicTest {
            return dynamicTest(displayName) {
                val ast = parser.parse(statement).root
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
                val expected = case.schema.toIon()
                val actual = statement.root.type.toIon()
                assertEquals(expected, actual)
            }
        }
    }
}
