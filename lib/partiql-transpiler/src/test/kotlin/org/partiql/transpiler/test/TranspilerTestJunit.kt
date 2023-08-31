package org.partiql.transpiler.test

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.test.PlannerTest
import org.partiql.planner.test.PlannerTestLog
import org.partiql.planner.test.PlannerTestProvider
import org.partiql.planner.test.PlannerTestSuite
import org.partiql.planner.test.plugin.FsPlugin
import org.partiql.transpiler.PartiQLTranspiler
import org.partiql.transpiler.TranspilerProblem
import org.partiql.transpiler.targets.PartiQLTarget
import java.util.stream.Stream
import kotlin.io.path.toPath

class TranspilerTestJunit {

    @TestFactory
    fun mapSuitesToJunitTests(): Stream<DynamicNode> {
        val provider = PlannerTestProvider()
        return provider.suites().map { suiteNode(it) }
    }

    companion object {

        private val log = PlannerTestLog(System.out)

        private val catalogConfig = mapOf(
            "default" to ionStructOf(
                field("connector_name", ionString("fs")),
            )
        )

        private fun suiteNode(suite: PlannerTestSuite): DynamicContainer {
            val schemaRoot = PlannerTest::class.java.getResource("/catalogs")!!.toURI().toPath()
            val plugin = FsPlugin(schemaRoot)
            val transpiler = PartiQLTranspiler(listOf(plugin))
            val tests = suite.tests.map { (name, test) ->
                val testName = "${suite.name}__$name"
                val session = PartiQLPlanner.Session(
                    queryId = "q__$testName",
                    userId = "transpiler_test_runner",
                    currentCatalog = suite.session.catalog,
                    currentDirectory = suite.session.path,
                    catalogConfig = catalogConfig,
                )
                testNode(testName, transpiler, session, test)
            }
            return dynamicContainer(suite.name, tests.stream())
        }

        private fun testNode(
            displayName: String,
            transpiler: PartiQLTranspiler,
            session: PartiQLPlanner.Session,
            test: PlannerTest,
        ): DynamicTest {
            return dynamicTest(displayName) {
                val result = transpiler.transpile(test.statement, PartiQLTarget, session)
                log.debug("RESULT: ${result.output.value}")
                for (problem in result.problems) {
                    if (problem.level == TranspilerProblem.Level.ERROR) {
                        fail { result.problems.joinToString() }
                    }
                }
                log.debug("SCHEMA: ${result.output.schema}")
            }
        }
    }
}
