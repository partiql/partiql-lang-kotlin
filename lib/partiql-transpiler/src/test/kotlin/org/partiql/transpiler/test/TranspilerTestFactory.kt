package org.partiql.transpiler.test

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.test.PlannerTest
import org.partiql.planner.test.PlannerTestProvider
import org.partiql.planner.test.PlannerTestSuite
import org.partiql.planner.test.plugin.FsPlugin
import org.partiql.transpiler.PartiQLTranspiler
import org.partiql.transpiler.TpTarget
import java.util.stream.Stream
import kotlin.io.path.toPath

/**
 * Base test factory for a transpiler target.
 */
abstract class TranspilerTestFactory<T>(
    public val target: TpTarget<T>,
) {

    /**
     * OVERRIDE ME
     *
     * @param suiteKey  Unique suite key for assertion lookup
     * @param testKey   Unique test key (within suite) for assertion lookup
     * @param test      Test data class
     * @param result    Transpilation result
     */
    public abstract fun assert(
        suiteKey: String,
        testKey: String,
        test: PlannerTest,
        result: PartiQLTranspiler.Result<T>,
    )

    /**
     * OVERRIDE ME
     */
    public open val catalogConfig = mapOf(
        "default" to ionStructOf(
            field("connector_name", ionString("fs")),
        ),
    )

    @TestFactory
    public fun mapSuitesToJunitTests(): Stream<DynamicNode> {
        val provider = PlannerTestProvider()
        return provider.suites().map { suiteNode(it) }
    }

    private fun suiteNode(suite: PlannerTestSuite): DynamicContainer {
        val schemaRoot = PlannerTest::class.java.getResource("/catalogs")!!.toURI().toPath()
        val plugin = FsPlugin(schemaRoot)
        val transpiler = PartiQLTranspiler(listOf(plugin))
        val suiteKey = suite.name
        val tests = suite.tests.map { (testKey, test) ->
            val displayName = "${suiteKey}__$testKey"
            val session = PartiQLPlanner.Session(
                queryId = "q__$displayName",
                userId = "transpiler_test_runner",
                currentCatalog = suite.session.catalog,
                currentDirectory = suite.session.path,
                catalogConfig = catalogConfig,
            )
            dynamicTest(displayName) {
                val result = transpiler.transpile(test.statement, target, session)
                assert(suiteKey, testKey, test, result)
            }
        }
        return dynamicContainer(suite.name, tests.stream())
    }
}
